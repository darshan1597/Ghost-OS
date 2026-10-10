package com.ghostos.ghost_os.voice

import android.content.Context
import android.media.MediaPlayer
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.io.File
import java.util.concurrent.Executors

/** Offline voices only. The optional greeting file is never uploaded. */
class SpeechOutput(private val context: Context, private val modelAsset: String = "voice/ryan/en_US-ryan-medium.onnx", private val changed: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var completion: (() -> Unit)? = null
    private var player: MediaPlayer? = null
    @Volatile private var generation = 0L
    private val worker = Executors.newSingleThreadExecutor()
    private var synthesis: File? = null
    private var processed: File? = null
    var speaking = false
        private set
    var strength: Float = context.getSharedPreferences("ghost", Context.MODE_PRIVATE).getFloat("monsterStrength", .55f)
    var character: String = context.getSharedPreferences("ghost", Context.MODE_PRIVATE).getString("voiceCharacter", "man") ?: "man"
    var voices: List<Map<String, String>> = emptyList()
        private set
    var selectedVoice = ""
        private set
    private var androidAvailable = false
    private var androidDescription = "Checking offline English voice…"

    private val prefs = context.getSharedPreferences("ghost", Context.MODE_PRIVATE)
    var preset: String = prefs.getString("voicePreset", "cinematic") ?: "cinematic"
        private set
    private val neural = NeuralVoiceEngine(context, changed, modelAsset)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var focus: AudioFocusRequest? = null
    private var keepWarm = false
    private val idleRelease = Runnable { if (!speaking && !keepWarm) neural.releaseIdle() }
    var lastPlaybackEngine = "none"
        private set
    fun setWarm(active: Boolean) {
        keepWarm = active; handler.removeCallbacks(idleRelease)
        if (active) initialize() else handler.postDelayed(idleRelease, 90000)
    }
    val available get() = if (preset == "cinematic" && character == "man") neural.ready || androidAvailable else androidAvailable
    val description get() = if (preset == "cinematic" && character == "man") neural.error ?: if (neural.ready) "Ghost Cinematic Monster Male ready (bundled, offline)" else "Preparing bundled Ghost voice" else androidDescription
    fun metrics(): Map<String, Any> = mapOf("ready" to neural.ready, "loads" to neural.loads, "initializationMs" to neural.initializationMs, "firstAudioMs" to neural.firstAudioMs)
    fun initialize() { if (preset == "cinematic" && character == "man") neural.initialize() }
    fun setVoicePreset(value: String) {
        require(value in listOf("cinematic", "android"))
        stop(); preset = value; prefs.edit().putString("voicePreset", value).apply(); initialize(); changed()
    }

    init {
        context.cacheDir.listFiles()?.filter { it.isFile && it.name.endsWith(".wav") &&
            (it.name.startsWith("ghost_speech_") || it.name.startsWith("ghost_effect_")) }?.forEach { it.delete() }
        engine = TextToSpeech(context) { status ->
            handler.post {
                val tts = engine
                val installed = if (status == TextToSpeech.SUCCESS) tts?.voices?.filter {
                    it.locale.language == Locale.ENGLISH.language && !it.isNetworkConnectionRequired &&
                        it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
                }?.sortedBy { it.name } ?: emptyList() else emptyList()
                voices = installed.map { mapOf("id" to it.name, "label" to "${it.locale.toLanguageTag()} ? ${it.name}") }
                val saved = context.getSharedPreferences("ghost", Context.MODE_PRIVATE).getString("ttsVoice", "")
                val voice = installed.firstOrNull { it.name == saved } ?: installed.firstOrNull { it.name == tts?.defaultVoice?.name } ?: installed.firstOrNull()
                androidAvailable = voice != null && tts?.setVoice(voice) == TextToSpeech.SUCCESS
                tts?.setAudioAttributes(NeuralVoiceEngine.attributes())
                selectedVoice = voice?.name ?: ""
                androidDescription = if (androidAvailable) "Offline English voice ready" else "Install an offline English voice in Android speech settings"
                changed()
            }
        }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { handler.post { if (id == generation.toString() && synthesis == null) lastPlaybackEngine = "android" } }
            override fun onDone(id: String?) { handler.post {
                if (id != generation.toString()) return@post
                val source = synthesis
                if (source == null) finish() else processAndPlay(source, generation)
            } }
            @Deprecated("Android compatibility callback")
            override fun onError(id: String?) { handler.post { if (id == generation.toString()) finish() } }
        })
    }

    fun speak(text: String, greeting: Boolean = false, done: () -> Unit = {}) {
        stop()
        speaking = true
        handler.removeCallbacks(idleRelease)
        lastPlaybackEngine = "none"
        changed()
        completion = done
        val token = generation
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(NeuralVoiceEngine.attributes())
            .setOnAudioFocusChangeListener({ change -> if (change < 0 && token == generation) finish() }, handler).build()
        focus = request
        if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { finish(); return }
        if (greeting) {
            try {
                val file = context.assets.openFd("voice/greeting.mp3")
                val media = MediaPlayer()
                player = media
                media.setAudioAttributes(NeuralVoiceEngine.attributes())
                file.use { media.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                media.setOnCompletionListener { if (token == generation) finish() }
                media.setOnErrorListener { _, _, _ -> if (token == generation) finish(); true }
                media.prepare()
                media.start()
                lastPlaybackEngine = "recording"
                handler.postDelayed({ if (token == generation) finish() }, 15000)
                return
            } catch (_: Exception) {
                player?.release()
                player = null
            }
        }
        if (preset == "cinematic" && character == "man") {
            neural.speak(text, strength,
                done = { if (token == generation) { lastPlaybackEngine = "neural"; finish() } },
                fallback = { if (token == generation) speakAndroid(text, token) })
            handler.postDelayed({ if (token == generation) finish() }, 120000)
        } else speakAndroid(text, token)
    }

    private fun speakAndroid(text: String, token: Long) {
        if (!androidAvailable) { finish(); return }
        engine?.setPitch(if (character == "man") .68f - .14f * strength else 1f - .12f * strength)
        engine?.setSpeechRate(if (character == "man") .9f else .96f)
        val result = if (strength > 0f) {
            File(context.cacheDir, "ghost_speech_$token.wav").also { synthesis = it }.let {
                engine?.synthesizeToFile(text, null, it, token.toString())
            }
        } else engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, token.toString())
        if (result != TextToSpeech.SUCCESS) {
            finish()
            return
        }
        // Ensure recognition resumes even if the installed TTS engine fails to signal completion.
        handler.postDelayed({ if (token == generation) finish() }, 45000)
    }

    fun selectVoice(id: String) {
        val voice = engine?.voices?.firstOrNull { it.name == id && !it.isNetworkConnectionRequired &&
            it.locale.language == "en" && it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true }
        require(voice != null) { "That offline English voice is not installed." }
        require(engine?.setVoice(voice) == TextToSpeech.SUCCESS) { "The voice could not be selected." }
        selectedVoice = id
        context.getSharedPreferences("ghost", Context.MODE_PRIVATE).edit().putString("ttsVoice", id).apply()
        changed()
    }

    private fun processAndPlay(source: File, token: Long) {
        val output = File(context.cacheDir, "ghost_effect_$token.wav")
        val amount = strength
        worker.execute {
            val file = try { if (WaveProcessor.process(source, output, amount)) output else source } catch (_: Exception) { source }
            handler.post {
                if (token != generation) { source.delete(); output.delete(); return@post }
                processed = output
                try {
                    val media = MediaPlayer()
                    player = media
                    media.setAudioAttributes(NeuralVoiceEngine.attributes())
                    media.setDataSource(file.absolutePath)
                    media.setOnPreparedListener { if (token == generation) { lastPlaybackEngine = "android"; it.start() } }
                    media.setOnCompletionListener { if (token == generation) finish() }
                    media.setOnErrorListener { _, _, _ -> if (token == generation) finish(); true }
                    media.prepareAsync()
                } catch (_: Exception) { finish() }
            }
        }
    }

    private fun finish() {
        val callback = completion
        completion = null
        generation++
        neural.stop()
        focus?.let { audioManager.abandonAudioFocusRequest(it) }; focus = null
        player?.release()
        player = null
        engine?.stop()
        synthesis?.delete(); synthesis = null
        processed?.delete(); processed = null
        speaking = false
        if (!keepWarm) { handler.removeCallbacks(idleRelease); handler.postDelayed(idleRelease, 90000) }
        changed()
        callback?.invoke()
    }

    fun dispose() { stop(); handler.removeCallbacks(idleRelease); neural.dispose(); engine?.shutdown(); engine = null; worker.shutdown() }

    /** Stops spoken output and completes the turn so listening resumes normally. */
    fun interrupt() { if (speaking) finish() }

    fun stop() {
        generation++
        neural.stop()
        focus?.let { audioManager.abandonAudioFocusRequest(it) }; focus = null
        completion = null
        player?.release()
        player = null
        engine?.stop()
        synthesis?.delete(); synthesis = null
        processed?.delete(); processed = null
        speaking = false
        changed()
    }
}
