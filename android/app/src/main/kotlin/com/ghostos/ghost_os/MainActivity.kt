package com.ghostos.ghost_os

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.net.Uri
import com.ghostos.ghost_os.core.GhostRuntime
import com.ghostos.ghost_os.voice.GhostListeningService
import com.ghostos.ghost_os.link.GhostLink
import com.ghostos.ghost_os.link.ControllerLinkService
import com.ghostos.ghost_os.emergency.EmergencyPhraseStore
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private var permissionResult: MethodChannel.Result? = null
    private var contactPermissionResult: MethodChannel.Result? = null
    private var bluetoothPermissionResult: MethodChannel.Result? = null
    private var emergencyPermissionResult: MethodChannel.Result? = null
    private var callPermissionResult: MethodChannel.Result? = null
    private var observer: ((Map<String, Any?>) -> Unit)? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        GhostRuntime.init(this)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "ghost/methods").setMethodCallHandler { call, result ->
            try {
                when (call.method) {
                    "snapshot" -> result.success(GhostRuntime.snapshot())
                    "setAssistantName" -> { GhostRuntime.setAssistantName(call.argument<String>("name") ?: "Ghost"); result.success(null) }
                    "downloadOfflineAi" -> { GhostRuntime.downloadOfflineAi(); result.success(null) }
                    "cancelOfflineAiDownload" -> { GhostRuntime.cancelOfflineAiDownload(); result.success(null) }
                    "removeOfflineAi" -> { GhostRuntime.removeOfflineAi(); result.success(null) }
                    "setOfflineAiEnabled" -> { GhostRuntime.setOfflineAiEnabled(call.argument<Boolean>("enabled") == true); result.success(null) }
                    "refreshOfflineAi" -> { GhostRuntime.refreshOfflineAi(); result.success(null) }
                    "linkRefresh" -> {
                        val required = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= 31) required.add(Manifest.permission.BLUETOOTH_CONNECT)
                        if (Build.VERSION.SDK_INT >= 33) required.add(Manifest.permission.POST_NOTIFICATIONS)
                        val missing = required.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
                        if (missing.isEmpty()) { GhostLink.refresh(); result.success(null) }
                        else { check(bluetoothPermissionResult == null); bluetoothPermissionResult = result; requestPermissions(missing.toTypedArray(), 44) }
                    }
                    "linkBluetoothSettings" -> { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); result.success(null) }
                    "linkConnect" -> {
                        GhostLink.refresh()
                        val address = call.argument<String>("address") ?: ""
                        require(GhostLink.devices.any { it["address"] == address }) { "Select a paired phone first." }
                        startForegroundService(Intent(this, ControllerLinkService::class.java).putExtra("address", address))
                        result.success(null)
                    }
                    "linkDisconnect" -> { GhostLink.disconnect(); stopService(Intent(this, ControllerLinkService::class.java)); result.success(null) }
                    "startListening" -> requestListening(result)
                    "stopListening" -> {
                        GhostRuntime.cancel()
                        stopService(Intent(this, GhostListeningService::class.java))
                        GhostRuntime.setState("Listening stopped", false)
                        result.success(null)
                    }
                    "submit" -> { GhostRuntime.submit(call.argument<String>("text") ?: ""); result.success(null) }
                    "setVoiceMode" -> { GhostRuntime.setVoiceMode(call.argument<String>("mode") ?: "brief"); result.success(null) }
                    "setMonsterStrength" -> { GhostRuntime.setMonsterStrength((call.argument<Number>("strength") ?: .55).toFloat()); result.success(null) }
                    "speakNow" -> { GhostRuntime.speakNow(); result.success(null) }
                    "setVoiceCharacter" -> { GhostRuntime.setVoiceCharacter(call.argument<String>("character") ?: "man"); result.success(null) }
                    "setVoicePreset" -> { GhostRuntime.setVoicePreset(call.argument<String>("preset") ?: "cinematic"); result.success(null) }
                    "initializeVoice" -> { GhostRuntime.initializeVoice(); result.success(null) }
                    "stopVoice" -> { GhostRuntime.stopVoice(); result.success(null) }
                    "selectVoice" -> { GhostRuntime.selectVoice(call.argument<String>("id") ?: ""); result.success(null) }
                    "chooseContact" -> { GhostRuntime.chooseContact(call.argument<String>("id") ?: ""); result.success(null) }
                    "requestContacts" -> {
                        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) { GhostRuntime.environmentChanged(); result.success(null) }
                        else {
                            check(contactPermissionResult == null) { "A contacts permission request is already open." }
                            contactPermissionResult = result
                            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 43)
                        }
                    }
                    "requestCallAccess" -> {
                        val missing = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS)
                            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
                        if (missing.isEmpty()) { GhostRuntime.environmentChanged(); result.success(null) }
                        else { check(callPermissionResult == null); callPermissionResult = result; requestPermissions(missing.toTypedArray(), 46) }
                    }
                    "setNotificationReading" -> { GhostRuntime.setNotificationReading(call.argument<Boolean>("enabled") == true); result.success(null) }
                    "setDrivingMode" -> { GhostRuntime.setDrivingMode(call.argument<Boolean>("enabled") == true); result.success(null) }
                    "setPreferBluetooth" -> {
                        val enabled = call.argument<Boolean>("enabled") == true
                        if (enabled && Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            check(bluetoothPermissionResult == null); bluetoothPermissionResult = result
                            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 44)
                        } else { GhostRuntime.setPreferBluetooth(enabled); result.success(null) }
                    }
                    "runRoutine" -> { GhostRuntime.runRoutine("Run routine", call.argument<String>("name") ?: ""); result.success(null) }
                    "deleteRoutine" -> { GhostRuntime.deleteRoutine(call.argument<String>("name") ?: ""); result.success(null) }
                    "notificationAccessSettings" -> { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")); result.success(null) }
                    "configureEmergency" -> {
                        GhostRuntime.configureEmergency(
                            call.argument<String>("phrase") ?: "",
                            call.argument<String>("remotePhrase") ?: "",
                            call.argument<String>("stopPhrase") ?: "",
                            call.argument<String>("phone") ?: "",
                            call.argument<String>("message") ?: ""
                        ); result.success(null)
                    }
                    "requestEmergencyPermissions" -> requestEmergencyPermissions(result)
                    "emergencyLocationSettings" -> { openAppSettings(); result.success(null) }
                    "setEmergencyEnabled" -> { GhostRuntime.setEmergencyEnabled(call.argument<Boolean>("enabled") == true); result.success(null) }
                    "stopEmergency" -> { GhostRuntime.stopEmergency(); result.success(null) }
                    "clearEmergency" -> { GhostRuntime.clearEmergency(); result.success(null) }
                    "deleteReminder" -> { GhostRuntime.deleteReminder((call.argument<Number>("id") ?: -1).toLong()); result.success(null) }
                    "exactAlarmSettings" -> {
                        if (Build.VERSION.SDK_INT >= 31) startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                        result.success(null)
                    }
                    "setAutoSend" -> {
                        val enabled = call.argument<Boolean>("enabled") == true
                        GhostRuntime.setAutoSend(enabled)
                        if (enabled && WhatsAppAutomation.sender == null) startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        result.success(null)
                    }
                    "accessibilitySettings" -> { check(WhatsAppAutomation.available) { "Automatic Send is not included in this build." }; startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); result.success(null) }
                    "approve" -> { GhostRuntime.approve(call.argument<String>("id") ?: ""); result.success(null) }
                    "cancel" -> { GhostRuntime.cancel(); result.success(null) }
                    "clearHistory" -> { GhostRuntime.clearHistory(); result.success(null) }
                    "scanApps" -> { GhostRuntime.scanApps(); result.success(null) }
                    "openPackage" -> { GhostRuntime.openPackage(call.argument<String>("package") ?: ""); result.success(null) }
                    "setBubbleSize" -> { GhostRuntime.setBubbleSize(call.argument<Int>("size") ?: 48); result.success(null) }
                    "setBubbleFade" -> { GhostRuntime.setBubbleFade(call.argument<Boolean>("enabled") == true); result.success(null) }
                    "setWakeSensitivity" -> { GhostRuntime.setWakeSensitivity(call.argument<String>("value") ?: "balanced"); result.success(null) }
                    "setSpeechPause" -> { GhostRuntime.setSpeechPause(call.argument<String>("value") ?: "balanced"); result.success(null) }
                    "setConversationFollowUps" -> { GhostRuntime.setConversationFollowUps(call.argument<Boolean>("enabled") == true); result.success(null) }
                    "setVoiceInterruption" -> { GhostRuntime.setVoiceInterruption(call.argument<Boolean>("enabled") == true); result.success(null) }
                    "setHandsFree" -> {
                        val enabled = call.argument<Boolean>("enabled") == true
                        GhostRuntime.setHandsFree(enabled)
                        if (enabled && !Settings.canDrawOverlays(this)) {
                            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                        }
                        result.success(null)
                    }
                    "previewVoice" -> { GhostRuntime.say("Hey boss. Ghost is ready. What shall we do?"); result.success(null) }
                    "speechSettings" -> { startActivity(Intent("com.android.settings.TTS_SETTINGS")); result.success(null) }
                    "batterySettings" -> { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); result.success(null) }
                    else -> result.notImplemented()
                }
            } catch (e: Exception) { result.error("ghost_error", e.localizedMessage ?: "The action failed.", null) }
        }
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, "ghost/events").setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, sink: EventChannel.EventSink) {
                observer?.let { GhostRuntime.removeObserver(it) }
                observer = { snapshot: Map<String, Any?> -> sink.success(snapshot) }
                observer?.let { GhostRuntime.observe(it) }
            }
            override fun onCancel(arguments: Any?) {
                observer?.let { GhostRuntime.removeObserver(it) }; observer = null
            }
        })
    }

    private fun requestListening(result: MethodChannel.Result) {
        if (permissionResult != null) { result.error("busy", "A permission request is already open.", null); return }
        val required = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) required.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = required.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) { beginListening(); result.success(null) }
        else { permissionResult = result; requestPermissions(missing.toTypedArray(), 42) }
    }
    private fun beginListening() { startForegroundService(Intent(this, GhostListeningService::class.java)) }
    private fun requestEmergencyPermissions(result: MethodChannel.Result) {
        if (EmergencyPhraseStore.permissionsReady(this)) { GhostRuntime.environmentChanged(); result.success(null); return }
        val foreground = listOf(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val missing = foreground.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            check(emergencyPermissionResult == null) { "A permission request is already open." }
            emergencyPermissionResult = result; requestPermissions(missing.toTypedArray(), 45); return
        }
        openAppSettings(); result.success(null)
    }
    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 44) {
            val reply = bluetoothPermissionResult; bluetoothPermissionResult = null
            try { GhostLink.refresh(); GhostRuntime.setPreferBluetooth(true); reply?.success(null) }
            catch (e: Exception) { reply?.error("bluetooth_required", e.message ?: "Allow nearby devices to connect Ghost Link.", null) }
            return
        }
        if (requestCode == 43) {
            val contactResult = contactPermissionResult; contactPermissionResult = null
            GhostRuntime.environmentChanged()
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) contactResult?.success(null)
            else contactResult?.error("contacts_required", "Allow contacts to find WhatsApp recipients by name.", null)
            return
        }
        if (requestCode == 45) {
            val reply = emergencyPermissionResult; emergencyPermissionResult = null
            GhostRuntime.environmentChanged()
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                openAppSettings()
                reply?.success(null)
            } else reply?.error("emergency_permissions_required", "Allow SMS and precise location. Then choose Allow all the time in Android app permissions.", null)
            return
        }
        if (requestCode == 46) {
            val reply = callPermissionResult; callPermissionResult = null
            GhostRuntime.environmentChanged()
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) reply?.success(null)
            else reply?.error("call_permissions_required", "Allow contacts and phone calls to call saved contacts.", null)
            return
        }
        if (requestCode != 42) return
        val result = permissionResult; permissionResult = null
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            try { beginListening(); result?.success(null) }
            catch (e: Exception) { result?.error("listening_failed", e.localizedMessage, null) }
        } else result?.error("permission_required", "Enable microphone and notifications in Android app settings to use voice listening. Text commands still work.", null)
    }
    override fun onResume() {
        super.onResume(); GhostRuntime.attach(this)
        intent.getStringExtra("ghost_tab")?.let { GhostRuntime.requestScreen(it); intent.removeExtra("ghost_tab") }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent) }
    override fun onPause() { GhostRuntime.attach(null); super.onPause() }
    override fun onDestroy() {
        observer?.let { GhostRuntime.removeObserver(it) }
        permissionResult?.error("cancelled", "Permission request interrupted.", null); permissionResult = null
        contactPermissionResult?.error("cancelled", "Permission request interrupted.", null); contactPermissionResult = null
        bluetoothPermissionResult?.error("cancelled", "Permission request interrupted.", null); bluetoothPermissionResult = null
        emergencyPermissionResult?.error("cancelled", "Permission request interrupted.", null); emergencyPermissionResult = null
        callPermissionResult?.error("cancelled", "Permission request interrupted.", null); callPermissionResult = null
        super.onDestroy()
    }
}
