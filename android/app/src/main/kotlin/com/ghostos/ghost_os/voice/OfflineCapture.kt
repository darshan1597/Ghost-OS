package com.ghostos.ghost_os.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.media.audiofx.AcousticEchoCanceler
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

/** One microphone feeds a small wake grammar and a free command decoder. No recording files. */
class OfflineCapture(model: Model, private val command: Boolean, private val wakeSensitivity: String, private val assistantName: String,
    private val partial: (String) -> Unit, private val result: (RecognitionDecision, Boolean) -> Unit,
    private val failed: (Exception) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var stopped = false
    @Volatile private var paused = false
    @Volatile private var resetRequested = false
    private var recorder: AudioRecord? = null
    private val thread = Thread({
        var free: Recognizer? = null
        var wake: Recognizer? = null
        var gain: AutomaticGainControl? = null
        var noise: NoiseSuppressor? = null
        var echo: AcousticEchoCanceler? = null
        try {
            free = Recognizer(model, 16000f)
            if (command) {
                free.setMaxAlternatives(5)
                free.setWords(true)
            }
            val cleanName = assistantName.lowercase().replace(Regex("[^a-z ]"), " ").replace(Regex("\\s+"), " ").trim().ifEmpty { "ghost" }
            if (!command) {
                val primary = "hey $cleanName"
                val grammar = when (wakeSensitivity) {
                    "strict" -> org.json.JSONArray(listOf(primary, "[unk]")).toString()
                    "high" -> org.json.JSONArray(listOf(primary, "hey ghost", "hey ghosts", "hey goes", "a ghost", "[unk]").distinct()).toString()
                    else -> org.json.JSONArray(listOf(primary, "hey ghost", "hey ghosts", "[unk]").distinct()).toString()
                }
                wake = Recognizer(model, 16000f, grammar)
            }
            val minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(minimum > 0) { "16 kHz microphone capture is unavailable" }
            val audio = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum * 2, 8192))
            recorder = audio
            check(audio.state == AudioRecord.STATE_INITIALIZED) { "Microphone could not initialise" }
            if (AutomaticGainControl.isAvailable()) gain = AutomaticGainControl.create(audio.audioSessionId)?.apply { enabled = true }
            if (NoiseSuppressor.isAvailable()) noise = NoiseSuppressor.create(audio.audioSessionId)?.apply { enabled = true }
            if (AcousticEchoCanceler.isAvailable()) echo = AcousticEchoCanceler.create(audio.audioSessionId)?.apply { enabled = true }
            if (stopped) return@Thread
            audio.startRecording()
            check(audio.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone is busy" }
            val buffer = ShortArray(1600)
            var woke = false
            var lastPartial = ""
            while (!stopped) {
                val count = audio.read(buffer, 0, buffer.size)
                if (count <= 0) { if (stopped) break else error("Microphone read failed ($count)") }
                if (paused) continue
                if (resetRequested) { free.reset(); wake?.reset(); woke = false; lastPartial = ""; resetRequested = false }
                if (wake != null) {
                    val ended = wake.acceptWaveForm(buffer, count)
                    val guess = JSONObject(if (ended) wake.result else wake.partialResult)
                        .optString(if (ended) "text" else "partial")
                    val escaped = Regex.escape(cleanName).replace("\\ ", "\\s+")
                    val accepted = Regex(if (wakeSensitivity == "high") "\\b(?:hey (?:$escaped|ghosts?|goes)|a ghost)\\b" else "\\bhey (?:$escaped|ghosts?)\\b")
                    if (accepted.containsMatchIn(guess)) woke = true
                }
                // In wake mode the expensive free decoder sleeps until the tiny grammar
                // recognizer hears the wake phrase. This is the main steady-state battery saving.
                if (wake != null && !woke) continue
                if (free.acceptWaveForm(buffer, count)) {
                    val decision = RecognitionSelector.decide(free.result, command)
                    val detected = woke || Regex("\\bhey (?:${Regex.escape(cleanName)}|ghosts?)\\b").containsMatchIn(decision.text)
                    if (decision.text.isNotEmpty() || detected) main.post { if (!stopped && !paused) result(decision, detected) }
                    woke = false
                    wake?.reset()
                    lastPartial = ""
                } else {
                    val text = JSONObject(free.partialResult).optString("partial")
                    if (text != lastPartial) {
                        lastPartial = text
                        main.post { if (!stopped && !paused) partial(text) }
                    }
                }
            }
        } catch (e: Exception) { if (!stopped) main.post { if (!stopped) failed(e) } }
        finally {
            try { gain?.release() } catch (_: Exception) {}
            try { noise?.release() } catch (_: Exception) {}
            try { echo?.release() } catch (_: Exception) {}
            try { recorder?.release() } catch (_: Exception) {}
            recorder = null; free?.close(); wake?.close()
        }
    }, "GhostOfflineMic")

    fun start() { thread.start() }
    val finished get() = !thread.isAlive
    fun setPause(value: Boolean) { if (!value) resetRequested = true; paused = value }
    fun stop() {
        stopped = true
        try { recorder?.stop() } catch (_: Exception) {}
        // Decoder/model ownership stays on this thread; close the model only after this joins.
        thread.join(1500)
    }
    fun awaitStopped() { thread.join() }
}
