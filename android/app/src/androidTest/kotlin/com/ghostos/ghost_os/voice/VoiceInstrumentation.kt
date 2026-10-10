package com.ghostos.ghost_os.voice

import android.app.Instrumentation
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.io.File

/** USB-invoked device checks; no test endpoint is exposed by the production APK. */
class VoiceInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val report = StringBuilder()
        var speech: SpeechOutput? = null
        val prefs = targetContext.getSharedPreferences("ghost", 0)
        val originalPreset = prefs.getString("voicePreset", null)
        val originalCharacter = prefs.getString("voiceCharacter", null)
        val originalMode = prefs.getString("voiceMode", null)
        var startedListening = false
        var resultCode = Activity.RESULT_CANCELED
        try {
            startActivitySync(Intent(targetContext, com.ghostos.ghost_os.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            // Stop any application reply while the separate test instance exercises playback.
            runOnMainSync { com.ghostos.ghost_os.core.GhostRuntime.stopVoice() }
            fun awaitReady(output: SpeechOutput) {
                val deadline = SystemClock.elapsedRealtime() + 45000
                while (!output.available && SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
                check(output.available) { "No offline voice available" }
            }
            runOnMainSync { speech = SpeechOutput(targetContext) {}; speech!!.character = "man"; speech!!.setVoicePreset("cinematic") }
            val output = speech!!
            val deadline = SystemClock.elapsedRealtime() + 45000
            while (output.metrics()["ready"] != true && SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
            check(output.metrics()["ready"] == true) { output.description }
            fun speak(text: String, greeting: Boolean = false): Long {
                val done = CountDownLatch(1); val start = SystemClock.elapsedRealtime()
                runOnMainSync { output.speak(text, greeting) { done.countDown() } }
                check(done.await(45, TimeUnit.SECONDS)) { "Speech timed out" }
                check(output.lastPlaybackEngine != "none") { "Audio did not start (focus or synthesis failure)" }
                return SystemClock.elapsedRealtime() - start
            }
            report.appendLine("neural_ready ${output.metrics()}")
            for (text in listOf("Hey boss.", "Your test code is ${SystemClock.elapsedRealtime() % 10000}. The blue moon rises over Mysore.", "Hey boss.")) {
                val elapsed = speak(text)
                check(output.metrics()["firstAudioMs"] as Long > 0)
                report.appendLine("speech_done elapsed_ms=$elapsed ${output.metrics()}")
            }
            check(output.metrics()["loads"] == 1) { "Model repeatedly reloaded" }
            var staleCompleted = false
            runOnMainSync { output.speak("This is a long response. ".repeat(30)) { staleCompleted = true } }
            Thread.sleep(150)
            speak("Interrupted. Ready, boss.")
            check(!staleCompleted) { "Cancelled response completed" }
            report.appendLine("interrupt_pass")
            repeat(12) { index -> runOnMainSync { output.speak("Rapid test $index") }; Thread.sleep(30) }
            speak("Rapid command check complete.")
            report.appendLine("rapid_commands_pass loads=${output.metrics()["loads"]}")
            speak("Hey boss", true) // Missing recording must use generated greeting.
            report.appendLine("generated_greeting_pass (no supplied recording)")
            runOnMainSync { output.setVoicePreset("android") }
            awaitReady(output)
            speak("Android offline fallback preview.")
            check(output.lastPlaybackEngine == "android")
            check(prefs.getString("voicePreset", "") == "android")
            report.appendLine("android_fallback_selection_pass")
            runOnMainSync { output.dispose(); speech = SpeechOutput(targetContext, "voice/missing-test-model.onnx") {}; speech!!.character = "man"; speech!!.setVoicePreset("cinematic") }
            val failed = speech!!; awaitReady(failed)
            val fallbackDone = CountDownLatch(1)
            runOnMainSync { failed.speak("The offline Android fallback is working.") { fallbackDone.countDown() } }
            check(fallbackDone.await(30, TimeUnit.SECONDS))
            check(failed.lastPlaybackEngine == "android")
            check(failed.description.contains("unavailable"))
            report.appendLine("missing_model_fallback_pass")
            // Exercise the actual response coordinator while Vosk owns the microphone.
            runOnMainSync {
                com.ghostos.ghost_os.core.GhostRuntime.setVoiceCharacter("man")
                com.ghostos.ghost_os.core.GhostRuntime.setVoicePreset("cinematic")
                com.ghostos.ghost_os.core.GhostRuntime.setVoiceMode("full")
                targetContext.startForegroundService(Intent(targetContext, GhostListeningService::class.java))
                startedListening = true
            }
            val listeningDeadline = SystemClock.elapsedRealtime() + 45000
            fun snapshot(): Map<String, Any?> {
                var value: Map<String, Any?> = emptyMap()
                runOnMainSync { value = com.ghostos.ghost_os.core.GhostRuntime.snapshot() }
                return value
            }
            while (!snapshot()["status"].toString().contains("Listening for Hey Ghost") && SystemClock.elapsedRealtime() < listeningDeadline) Thread.sleep(100)
            check(snapshot()["status"].toString().contains("Listening for Hey Ghost")) { snapshot()["status"].toString() }
            val coordinatedReply = CountDownLatch(1)
            runOnMainSync { com.ghostos.ghost_os.core.GhostRuntime.say("Offline voice and microphone check complete.") { coordinatedReply.countDown() } }
            check(coordinatedReply.await(30, TimeUnit.SECONDS))
            check(snapshot()["listening"] == true)
            report.appendLine("vosk_service_and_coordinated_reply_pass")
            targetContext.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(2500)
            check(snapshot()["listening"] == true)
            report.appendLine("background_listening_service_pass (spoken wake accuracy requires human check)")
            File(targetContext.filesDir, "voice-device-test.txt").writeText(report.toString())
            resultCode = Activity.RESULT_OK
        } catch (t: Throwable) {
            report.appendLine("FAILED: ${t.stackTraceToString()}")
            File(targetContext.filesDir, "voice-device-test.txt").writeText(report.toString())
        } finally {
            runOnMainSync {
                speech?.dispose()
                if (startedListening) targetContext.stopService(Intent(targetContext, GhostListeningService::class.java))
            }
            prefs.edit().apply {
                if (originalPreset == null) remove("voicePreset") else putString("voicePreset", originalPreset)
                if (originalCharacter == null) remove("voiceCharacter") else putString("voiceCharacter", originalCharacter)
                if (originalMode == null) remove("voiceMode") else putString("voiceMode", originalMode)
            }.commit()
        }
        finish(resultCode, Bundle().apply { putString("stream", report.toString()) })
    }
}
