package com.ghostos.ghost_os.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import com.ghostos.ghost_os.MainActivity
import com.ghostos.ghost_os.core.GhostRuntime
import com.ghostos.ghost_os.core.WakeCommandExtractor
import org.vosk.Model
import org.vosk.android.StorageService

/** User-started foreground microphone service; no cloud audio or boot auto-start. */
class GhostListeningService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var model: Model? = null
    private var capture: OfflineCapture? = null
    private val retiredCaptures = mutableListOf<OfflineCapture>()
    private var wakeLock: PowerManager.WakeLock? = null
    private var destroyed = false
    private var generation = 0
    private var commandMode = false
    private var interruptionMode = false
    private var commandModeBeforeInterruption = false
    private var suspended = false
    private var timeout: Runnable? = null
    private var dispatch: Runnable? = null
    private val segments = mutableListOf<String>()
    private lateinit var bubble: AssistantBubble
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) bubble.hide()
            else handler.post { if (!destroyed) GhostRuntime.environmentChanged() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        GhostRuntime.init(this)
        bubble = AssistantBubble(this)
        GhostRuntime.refreshBubble = { bubble.refresh() }
        GhostRuntime.refreshAudioRoute = { AudioRouting.refresh(this, GhostRuntime.drivingMode() && GhostRuntime.preferBluetooth()) }
        GhostRuntime.refreshWakeRecognizer = { if (model != null && !commandMode && !destroyed) startCapture(false) }
        GhostRuntime.bubbleVisible = { bubble.visible }
        val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        else registerReceiver(screenReceiver, filter)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("ghost_listening", "Ghost listening", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            GhostRuntime.cancel()
            stopSelf()
            return START_NOT_STICKY
        }
        if (model != null || GhostRuntime.listening) return START_NOT_STICKY
        try {
            val open = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val stop = PendingIntent.getService(this, 2, Intent(this, GhostListeningService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val notification = Notification.Builder(this, "ghost_listening")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Ghost is listening")
                .setContentText("Say Hey ${GhostRuntime.assistantName()}. Tap Stop to release the microphone.")
                .setContentIntent(open).setOngoing(true).setVisibility(Notification.VISIBILITY_PRIVATE)
                .addAction(Notification.Action.Builder(null, "Stop", stop).build()).build()
            if (Build.VERSION.SDK_INT >= 29) startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else startForeground(1, notification)
            GhostRuntime.setState("Loading offline English model…", true)
            GhostRuntime.environmentChanged()
            AudioRouting.refresh(this, GhostRuntime.drivingMode() && GhostRuntime.preferBluetooth())
            wakeLock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Ghost:Listening").apply { acquire() }
            GhostRuntime.pauseRecognition = {
                suspended = true; capture?.setPause(true)
                timeout?.let { handler.removeCallbacks(it) }
            }
            GhostRuntime.resumeRecognition = {
                suspended = false
                if (interruptionMode) Unit
                else if (commandMode && model != null && !destroyed) startCapture(true)
                else if (capture == null && model != null && !destroyed) startCapture(false)
                else { capture?.setPause(false) }
            }
            GhostRuntime.beginInterruptionListening = {
                if (model != null && !destroyed) {
                    commandModeBeforeInterruption = commandMode
                    suspended = false
                    startCapture(true, interruption = true)
                }
            }
            GhostRuntime.endInterruptionListening = {
                if (interruptionMode && !destroyed) {
                    interruptionMode = false
                    stopCapture()
                    commandMode = commandModeBeforeInterruption
                }
            }
            GhostRuntime.stopListening = { stopSelf() }
            GhostRuntime.listenForCommand = {
                check(model != null && !destroyed) { "The offline model is still loading. Wait a moment, boss." }
                startCapture(true)
            }
            StorageService.unpack(this, "model-en-us", "model", { loaded ->
                if (destroyed) loaded.close()
                else { model = loaded; startCapture(false) }
            }, { error -> fail("Offline model could not load: ${error.message}") })
        } catch (e: Exception) { fail("Listening could not start: ${e.message}") }
        // Android must not restart microphone listening from an ineligible background state.
        return START_NOT_STICKY
    }

    private fun startCapture(command: Boolean, interruption: Boolean = false) {
        if (destroyed) return
        stopCapture()
        commandMode = command
        interruptionMode = interruption
        val token = generation
        try {
            val loaded = model ?: return
            capture = OfflineCapture(loaded, command, GhostRuntime.wakeSensitivity(), GhostRuntime.assistantName(), { text ->
                if (token == generation && !destroyed && !suspended) {
                    GhostRuntime.partial((segments + text).filter { it.isNotBlank() }.joinToString(" "))
                    if (commandMode && text.isNotEmpty()) {
                        dispatch?.let { handler.removeCallbacks(it) }; dispatch = null
                    }
                }
            }, { decision, wakeDetected -> result(decision, token, wakeDetected) },
                { error -> if (token == generation) fail("Microphone error: ${error.message}") })
            capture?.start()
            GhostRuntime.setState(if (command) "Listening for your command…" else "Listening for Hey ${GhostRuntime.assistantName()}", true)
            if (command && !interruption) {
                timeout = Runnable {
                    if (token == generation && !destroyed) {
                        startCapture(false)
                        GhostRuntime.say("I did not hear a command. Say Hey ${GhostRuntime.assistantName()} to try again.", "Try again, boss.")
                    }
                }.also { handler.postDelayed(it, 20000) }
            }
        } catch (e: Exception) { fail("Offline recognition failed: ${e.message}") }
    }

    private fun result(decision: RecognitionDecision, token: Int, wakeDetected: Boolean) {
        if (token != generation || destroyed || suspended) return
        val text = decision.text
        if (interruptionMode) {
            if (Regex("\\bghost (?:stop|quiet)|\\bstop talking\\b|\\bbe quiet\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                interruptionMode = false
                GhostRuntime.interruptVoice()
            }
            return
        }
        if (!commandMode) {
            WakeCommandExtractor.extract(text, wakeDetected, GhostRuntime.assistantName())?.let { wake(it) }
            return
        }
        if (text.isBlank()) return
        segments.add(text)
        dispatch?.let { handler.removeCallbacks(it) }
        dispatch = Runnable {
            if (token == generation && !destroyed && !suspended) {
                val complete = segments.joinToString(" ")
                startCapture(false)
                GhostRuntime.submitRecognized(decision.copy(text = complete))
            }
        }.also { handler.postDelayed(it, GhostRuntime.commandPauseMs()) }
    }

    private fun wake(inlineCommand: String = "") {
        if (inlineCommand.isNotEmpty()) {
            startCapture(false)
            GhostRuntime.submit(inlineCommand)
        } else {
            stopCapture()
            GhostRuntime.setState("Hey boss. Listen for my greeting, then speak.", true)
            GhostRuntime.say("Hey boss", greeting = true) { if (!destroyed) startCapture(true) }
        }
    }
    private fun stopCapture() {
        generation++
        dispatch?.let { handler.removeCallbacks(it) }; dispatch = null
        segments.clear()
        timeout?.let { handler.removeCallbacks(it) }; timeout = null
        retiredCaptures.removeAll { it.finished }
        capture?.let { it.stop(); if (!it.finished) retiredCaptures.add(it) }
        capture = null
    }
    private fun fail(message: String) {
        if (destroyed) return
        GhostRuntime.setState(message, false)
        stopSelf()
    }
    override fun onDestroy() {
        destroyed = true
        stopCapture()
        val loaded = model; model = null
        val retired = retiredCaptures.toList(); retiredCaptures.clear()
        Thread({ retired.forEach { it.awaitStopped() }; loaded?.close() }, "GhostModelCleanup").start()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        GhostRuntime.pauseRecognition = null
        GhostRuntime.resumeRecognition = null
        GhostRuntime.stopListening = null
        GhostRuntime.listenForCommand = null
        GhostRuntime.beginInterruptionListening = null
        GhostRuntime.endInterruptionListening = null
        bubble.dispose()
        unregisterReceiver(screenReceiver)
        GhostRuntime.refreshBubble = null
        GhostRuntime.refreshAudioRoute = null
        GhostRuntime.refreshWakeRecognizer = null
        AudioRouting.release(this)
        GhostRuntime.bubbleVisible = null
        GhostRuntime.silence()
        if (GhostRuntime.listening) GhostRuntime.setState("Listening stopped", false)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
