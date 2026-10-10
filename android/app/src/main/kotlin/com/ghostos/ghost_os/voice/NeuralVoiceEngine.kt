package com.ghostos.ghost_os.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** One serial owner of the native model and PCM playback. No network or model extraction. */
class NeuralVoiceEngine(private val context: Context, private val changed: () -> Unit, private val modelAsset: String = "voice/ryan/en_US-ryan-medium.onnx") {
    private val main = Handler(Looper.getMainLooper())
    private val serial = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
    private val generation = AtomicLong()
    private var model: OfflineTts? = null // accessed only by serial worker
    private val cache = linkedMapOf<String, FloatArray>()
    @Volatile var ready = false; private set
    @Volatile var error: String? = null; private set
    @Volatile private var disposed = false
    @Volatile var initializationMs = 0L; private set
    @Volatile var firstAudioMs = 0L; private set
    @Volatile var loads = 0; private set

    fun initialize() { if (!disposed) serial.execute { runCatching { ensureModel() }.onFailure { failed(it) } } }

    private fun ensureModel(): OfflineTts {
        model?.let { return it }
        val start = SystemClock.elapsedRealtime()
        context.assets.open(modelAsset).use { check(it.read() >= 0) { "Empty neural voice model" } }
        // eSpeak needs filesystem access. Only its small data directory is extracted;
        // the 63 MB ONNX and tokens are read directly from the APK AssetManager.
        val data = File(context.filesDir, "ghost-voice/espeak-v1")
        val marker = File(data, ".complete")
        if (!marker.exists()) {
            fun copy(asset: String, target: File) {
                val children = context.assets.list(asset) ?: emptyArray()
                if (children.isEmpty()) {
                    target.parentFile?.mkdirs()
                    context.assets.open(asset).use { source -> target.outputStream().use { source.copyTo(it) } }
                } else { target.mkdirs(); children.forEach { copy("$asset/$it", File(target, it)) } }
            }
            copy("voice/ryan/espeak-ng-data", data)
            marker.writeText("sherpa-onnx-1.12.20/ryan-medium")
        }
        val loaded = OfflineTts(context.assets, OfflineTtsConfig(
            model = OfflineTtsModelConfig(vits = OfflineTtsVitsModelConfig(
                model = modelAsset,
                tokens = "voice/ryan/tokens.txt", dataDir = data.absolutePath,
            ), numThreads = 2, debug = false, provider = "cpu"), maxNumSentences = 1,
        ))
        check(loaded.sampleRate() == 22050) { "Unexpected Ryan sample rate" }
        model = loaded; loads++; ready = true; error = null
        initializationMs = SystemClock.elapsedRealtime() - start
        Log.i("GhostVoice", "model_ready init_ms=$initializationMs loads=$loads")
        main.post(changed)
        return loaded
    }

    private fun failed(t: Throwable) {
        error = "Bundled voice unavailable; using Android offline voice"
        Log.e("GhostVoice", "Neural voice failed", t); main.post(changed)
    }

    fun speak(text: String, strength: Float, done: () -> Unit, fallback: () -> Unit) {
        stop()
        val token = generation.get()
        val started = SystemClock.elapsedRealtime()
        serial.execute {
            var track: AudioTrack? = null
            var played = false
            var frames = 0L
            fun current() = !disposed && generation.get() == token
            try {
                if (!current()) return@execute
                val tts = ensureModel()
                if (!current()) return@execute
                val rate = tts.sampleRate()
                val buffer = maxOf(AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT), rate / 5 * 4)
                val playback = AudioTrack.Builder().setAudioAttributes(attributes())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_FLOAT).build())
                    .setBufferSizeInBytes(buffer).setTransferMode(AudioTrack.MODE_STREAM).build()
                track = playback
                check(playback.state == AudioTrack.STATE_INITIALIZED)
                fun play(raw: FloatArray): Int {
                    if (!current()) return 0
                    val pcm = CinematicEffects.process(raw, rate, strength)
                    var offset = 0
                    while (offset < pcm.size && current()) {
                        val count = playback.write(pcm, offset, minOf(2048, pcm.size - offset), AudioTrack.WRITE_NON_BLOCKING)
                        check(count >= 0) { "Audio output failed: $count" }
                        if (count == 0) Thread.sleep(5) else {
                            offset += count; frames += count
                            if (!played) {
                                playback.play(); played = true
                                firstAudioMs = SystemClock.elapsedRealtime() - started
                                Log.i("GhostVoice", "audio_started first_audio_ms=$firstAudioMs cache=${cache.containsKey(text)}")
                                main.post(changed)
                            }
                        }
                    }
                    return if (current()) 1 else 0
                }
                val saved = cache[text]
                if (saved != null) play(saved) else {
                    // JNI v1.12.20 looks up invoke(float[]): Integer by signature.
                    // Kotlin 2.4 indy lambdas only expose the erased Object bridge.
                    // A concrete Function1 class retains the typed, boxed callback.
                    var callbackFailure: Throwable? = null
                    val generated = tts.generateWithCallback(text, speed = CinematicEffects.ghost.speed,
                        callback = PcmCallback { pcm ->
                            try { play(pcm) } catch (t: Throwable) { callbackFailure = t; 0 }
                        })
                    callbackFailure?.let { throw it }
                    if (current() && text.length <= 160 && generated.samples.size <= 250000) {
                        while (cache.isNotEmpty() && (cache.size >= 8 || cache.values.sumOf { it.size } + generated.samples.size > 500000)) cache.remove(cache.keys.first())
                        cache[text] = generated.samples
                    }
                }
                check(played || !current()) { "No audio generated" }
                val deadline = SystemClock.elapsedRealtime() + 15000
                while (current() && (playback.playbackHeadPosition.toLong() and 0xffffffffL) < frames) {
                    check(SystemClock.elapsedRealtime() < deadline) { "Audio output stalled" }; Thread.sleep(10)
                }
                if (current()) main.post { if (current()) done() }
            } catch (t: Throwable) {
                if (current()) {
                    failed(t)
                    main.post { if (current()) { if (played) done() else fallback() } }
                }
            } finally {
                track?.let { runCatching { it.pause(); it.flush(); it.release() } }
            }
        }
    }
    fun stop() { generation.incrementAndGet(); serial.queue.clear() }
    fun releaseIdle() {
        if (!disposed) serial.execute { cache.clear(); model?.free(); model = null; ready = false; main.post(changed) }
    }
    fun dispose() {
        stop(); disposed = true
        serial.execute { cache.clear(); model?.free(); model = null; ready = false }
        serial.shutdown()
    }
    companion object {
        fun attributes(): AudioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    }
}

/** Must retain its typed invoke signature: called reflectively from the sherpa JNI library. */
internal class PcmCallback(private val receive: (FloatArray) -> Int) : (FloatArray) -> Int {
    override fun invoke(samples: FloatArray): Int = receive(samples)
}
