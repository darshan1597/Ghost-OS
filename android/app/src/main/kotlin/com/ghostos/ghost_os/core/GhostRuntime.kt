package com.ghostos.ghost_os.core

import android.app.Activity
import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import com.ghostos.ghost_os.MainActivity
import com.ghostos.ghost_os.WhatsAppAutomation
import com.ghostos.ghost_os.UnlockActivity
import com.ghostos.ghost_os.voice.SpeechOutput
import com.ghostos.ghost_os.voice.RecognitionDecision
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.UUID
import com.ghostos.ghost_os.link.GhostLink
import com.ghostos.link.RemoteCommand
import com.ghostos.ghost_os.emergency.EmergencyLocationService
import com.ghostos.ghost_os.emergency.EmergencyPhraseMatch
import com.ghostos.ghost_os.emergency.EmergencyPhraseStore
import com.ghostos.ghost_os.reminders.ReminderStore
import com.ghostos.ghost_os.notifications.NotificationCenter
import com.ghostos.ghost_os.voice.AudioRouting
import java.text.DateFormat
import java.util.Date

/** Process-wide coordinator: tasks do not depend on a live Flutter screen. */
object GhostRuntime {
    private lateinit var context: Context
    private lateinit var speech: SpeechOutput
    private var host = WeakReference<Activity>(null)
    private val observers = mutableSetOf<(Map<String, Any?>) -> Unit>()
    private val history = mutableListOf<Map<String, Any>>()
    private var pendingIntent: Intent? = null
    private var pendingOperation: (() -> Unit)? = null
    private var pendingId: String? = null
    private var pendingTitle: String? = null
    private var pendingDetail: String? = null
    private var pendingKind: String? = null
    private var pendingResult: String? = null
    private var pendingDeadline = 0L
    private var installedApps = emptyList<InstalledApp>()
    var refreshBubble: (() -> Unit)? = null
    var refreshAudioRoute: (() -> Unit)? = null
    var refreshWakeRecognizer: (() -> Unit)? = null
    var bubbleVisible: (() -> Boolean)? = null
    var listening = false
        private set
    private var status = "Ready"
    private var transcript = ""
    private var screenRequest: Map<String, String>? = null
    fun requestScreen(screen: String) { screenRequest = mapOf("id" to UUID.randomUUID().toString(), "screen" to screen); publish() }
    private var waitingForAlarmTime = false
    private var contactChoices = emptyList<PhoneContact>()
    private var contactMessage: GhostCommand.Message? = null
    private var contactDeadline = 0L
    private var messagePlan: PhoneContact? = null
    private var messageBody = ""
    private var lastAppName: String? = null
    private var lastContactName: String? = null
    private var activeTask: RoutineTask? = null
    var listenForCommand: (() -> Unit)? = null
    var pauseRecognition: (() -> Unit)? = null
    var resumeRecognition: (() -> Unit)? = null
    var stopListening: (() -> Unit)? = null
    var beginInterruptionListening: (() -> Unit)? = null
    var endInterruptionListening: (() -> Unit)? = null
    private var clarification: RecognitionDecision? = null
    private val prefs get() = context.getSharedPreferences("ghost", Context.MODE_PRIVATE)

    fun init(ctx: Context) {
        if (::context.isInitialized) return
        context = ctx.applicationContext
        OfflineAi.init(context)
        OfflineAi.changed = { publish() }
        GhostLink.init(context)
        NotificationCenter.changed = { publish() }
        GhostLink.changed = { publish() }
        GhostLink.completed = { message -> record("Other phone", message); say(message) }
        if (!WhatsAppAutomation.available) prefs.edit().putBoolean("autoSend", false).apply()
        speech = SpeechOutput(context) { publish() }
        speech.initialize()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("ghost_actions", "Ghost action requests", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun attach(activity: Activity?) {
        host = WeakReference(activity)
        speech.setWarm(activity != null || listening)
        refreshBubble?.invoke()
        if (activity != null) { scanApps(); continueAutomatic(); prepareEmergencyWhatsApp() }
    }
    fun isForeground() = host.get() != null
    fun observe(observer: (Map<String, Any?>) -> Unit) { observers.add(observer); observer(snapshot()) }
    fun removeObserver(observer: (Map<String, Any?>) -> Unit) { observers.remove(observer) }
    fun snapshot(): Map<String, Any?> = mapOf(
        "listening" to listening, "status" to status, "transcript" to transcript,
        "screenRequest" to screenRequest,
        "assistantName" to prefs.getString("assistantName", "Ghost"),
        "link" to GhostLink.snapshot(),
        "voiceMode" to prefs.getString("voiceMode", "brief"),
        "voiceStatus" to if (::speech.isInitialized) speech.description else "Checking voice…",
        "voiceReady" to (::speech.isInitialized && speech.available),
        "speaking" to (::speech.isInitialized && speech.speaking),
        "monsterStrength" to prefs.getFloat("monsterStrength", .55f),
        "voiceCharacter" to prefs.getString("voiceCharacter", "man"),
        "ttsVoices" to speech.voices, "ttsVoice" to speech.selectedVoice,
        "voicePreset" to speech.preset, "voiceMetrics" to speech.metrics(),
        "contactsAllowed" to PhoneContacts.allowed(context),
        "phoneCallAllowed" to (context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED),
        "autoSendAvailable" to WhatsAppAutomation.available,
        "autoSend" to (WhatsAppAutomation.available && prefs.getBoolean("autoSend", false)),
        "whatsappAccess" to (WhatsAppAutomation.sender != null),
        "contactChoices" to contactChoices.map { mapOf("id" to it.id, "name" to it.name, "phone" to it.phone) },
        "history" to history.toList(),
        "apps" to installedApps.map { mapOf("name" to it.name, "package" to it.packageName) },
        "bubbleSize" to bubbleSize(), "bubbleFade" to bubbleFade(),
        "wakeSensitivity" to wakeSensitivity(), "speechPause" to speechPause(),
        "conversationFollowUps" to conversationFollowUps(), "voiceInterruption" to voiceInterruption(),
        "notificationReading" to notificationReading(),
        "notificationAccess" to NotificationCenter.accessGranted(context),
        "notificationCount" to NotificationCenter.latest().size,
        "drivingMode" to drivingMode(), "preferBluetooth" to preferBluetooth(),
        "bluetoothAudio" to AudioRouting.snapshot(context),
        "routines" to RoutineStore(context).all().map { mapOf("name" to it.name, "steps" to it.steps) },
        "activeTask" to activeTask?.snapshot(),
        "handsFree" to handsFreeEnabled(), "overlayAllowed" to Settings.canDrawOverlays(context),
        "handsFreeReady" to (bubbleVisible?.invoke() == true),
        "emergency" to EmergencyPhraseStore.snapshot(context),
        "reminders" to ReminderStore(context).all().map { mapOf("id" to it.id, "text" to it.text, "triggerAt" to it.triggerAt) },
        "offlineAi" to OfflineAi.snapshot(),
        "pending" to pendingId?.let { mapOf("id" to it, "title" to pendingTitle, "detail" to pendingDetail, "kind" to pendingKind,
            "automatic" to (pendingKind?.let(ActionPolicy::requiresReview) != true)) }
    )
    private fun publish() { val value = snapshot(); observers.toList().forEach { it(value) } }
    fun setState(value: String, active: Boolean = listening) { status = value; listening = active; speech.setWarm(active || isForeground()); publish() }
    fun setAssistantName(value: String) {
        val name = value.trim().replace(Regex("\\s+"), " ").ifEmpty { "Ghost" }
        require(name.length <= 24 && name.none { it.isISOControl() }) { "Use a name up to 24 characters." }
        prefs.edit().putString("assistantName", name).apply()
        refreshWakeRecognizer?.invoke()
        publish()
    }
    fun assistantName() = prefs.getString("assistantName", "Ghost") ?: "Ghost"
    fun partial(value: String) { if (transcript != value) { transcript = value; publish() } }
    fun setVoiceMode(mode: String) {
        require(mode in listOf("off", "brief", "full"))
        prefs.edit().putString("voiceMode", mode).apply()
        if (mode == "off") { endInterruptionListening?.invoke(); speech.stop(); resumeRecognition?.invoke() }
        publish()
    }
    fun setMonsterStrength(value: Float) {
        val amount = value.coerceIn(0f, 1f)
        prefs.edit().putFloat("monsterStrength", amount).apply()
        speech.strength = amount
        publish()
    }
    fun setVoiceCharacter(character: String) {
        require(character in listOf("man", "woman"))
        prefs.edit().putString("voiceCharacter", character).apply()
        speech.character = character
        speech.initialize()
        publish()
    }
    fun setVoicePreset(preset: String) { speech.setVoicePreset(preset); resumeRecognition?.invoke() }
    fun initializeVoice() { speech.initialize() }
    fun stopVoice() { endInterruptionListening?.invoke(); speech.stop(); resumeRecognition?.invoke() }
    fun selectVoice(id: String) { speech.selectVoice(id) }
    fun setAutoSend(enabled: Boolean) {
        require(!enabled || WhatsAppAutomation.available) { "This build prepares WhatsApp drafts. Press Send in WhatsApp." }
        prefs.edit().putBoolean("autoSend", enabled).apply()
        if (!enabled) {
            WhatsAppAutomation.sender?.cancel(); messagePlan = null
            if (pendingKind == "whatsapp") clearPending()
        }
        publish()
    }
    fun speakNow() {
        require(listening && listenForCommand != null) { "Enable listening first, then tap Speak now." }
        endInterruptionListening?.invoke(); speech.stop(); resumeRecognition?.invoke(); listenForCommand?.invoke()
    }
    private fun ask(full: String) {
        say(full) { if (listening) listenForCommand?.invoke() }
    }
    fun say(full: String, brief: String = full, greeting: Boolean = false, followUp: Boolean = false, done: () -> Unit = {}) {
        val mode = prefs.getString("voiceMode", "brief")
        if (mode == "off") { done(); if (followUp && conversationFollowUps() && listening) listenForCommand?.invoke(); return }
        pauseRecognition?.invoke()
        if (voiceInterruption()) beginInterruptionListening?.invoke()
        speech.speak(if (mode == "full" && !drivingMode()) full else brief, greeting) {
            if (voiceInterruption()) endInterruptionListening?.invoke()
            resumeRecognition?.invoke()
            done()
            if (followUp && conversationFollowUps() && listening) listenForCommand?.invoke()
        }
    }
    fun interruptVoice() { speech.interrupt() }

    fun submitRecognized(decision: RecognitionDecision) {
        val answer = decision.text.trim()
        val pending = clarification
        if (pending != null) {
            clarification = null
            when (answer.lowercase(Locale.ROOT).trimEnd('.', '!', '?')) {
                "yes", "yeah", "correct", "that's right", "that is right" -> submit(pending.alternative ?: pending.text)
                "no", "nope", "wrong" -> ask("Okay, boss. Please say the command again.")
                else -> submit(answer)
            }
            return
        }
        if (decision.uncertain) {
            clarification = decision
            val alternative = decision.alternative
            if (alternative != null) ask("I heard ${decision.text}. Did you mean $alternative? Say yes, no, or repeat the command.")
            else ask("I heard ${decision.text}, but I am not confident. Please repeat the command, boss.")
            return
        }
        submit(answer)
    }
    private fun record(command: String, outcome: String) {
        history.add(0, mapOf("command" to command, "outcome" to outcome, "time" to System.currentTimeMillis()))
        if (history.size > 50) history.removeAt(history.lastIndex)
        publish()
    }
    fun clearHistory() { history.clear(); transcript = ""; publish() }
    fun downloadOfflineAi() = OfflineAi.download()
    fun cancelOfflineAiDownload() = OfflineAi.cancelDownload()
    fun removeOfflineAi() = OfflineAi.remove()
    fun setOfflineAiEnabled(enabled: Boolean) = OfflineAi.setEnabled(enabled)
    fun refreshOfflineAi() = OfflineAi.refresh()
    fun bubbleSize() = prefs.getInt("bubbleSize", 48).takeIf { it in listOf(40, 48, 56) } ?: 48
    fun bubbleFade() = prefs.getBoolean("bubbleFade", true)
    fun setBubbleSize(size: Int) { require(size in listOf(40, 48, 56)); prefs.edit().putInt("bubbleSize", size).apply(); refreshBubble?.invoke(); publish() }
    fun setBubbleFade(enabled: Boolean) { prefs.edit().putBoolean("bubbleFade", enabled).apply(); refreshBubble?.invoke(); publish() }
    fun wakeSensitivity() = prefs.getString("wakeSensitivity", "balanced") ?: "balanced"
    fun setWakeSensitivity(value: String) { require(value in listOf("strict", "balanced", "high")); prefs.edit().putString("wakeSensitivity", value).apply(); publish() }
    fun speechPause() = prefs.getString("speechPause", "balanced") ?: "balanced"
    fun commandPauseMs() = when (speechPause()) { "fast" -> 750L; "noisy" -> 1900L; else -> 1200L }
    fun setSpeechPause(value: String) { require(value in listOf("fast", "balanced", "noisy")); prefs.edit().putString("speechPause", value).apply(); publish() }
    fun conversationFollowUps() = prefs.getBoolean("conversationFollowUps", true)
    fun setConversationFollowUps(value: Boolean) { prefs.edit().putBoolean("conversationFollowUps", value).apply(); publish() }
    fun voiceInterruption() = prefs.getBoolean("voiceInterruption", true)
    fun setVoiceInterruption(value: Boolean) { prefs.edit().putBoolean("voiceInterruption", value).apply(); publish() }
    fun notificationReading() = prefs.getBoolean("notificationReading", false)
    fun setNotificationReading(value: Boolean) { prefs.edit().putBoolean("notificationReading", value).apply(); publish() }
    fun drivingMode() = prefs.getBoolean("drivingMode", false)
    fun preferBluetooth() = prefs.getBoolean("preferBluetooth", true)
    fun setDrivingMode(value: Boolean) {
        prefs.edit().putBoolean("drivingMode", value).apply()
        refreshAudioRoute?.invoke(); publish()
    }
    fun setPreferBluetooth(value: Boolean) {
        prefs.edit().putBoolean("preferBluetooth", value).apply()
        refreshAudioRoute?.invoke(); publish()
    }
    fun handsFreeEnabled() = prefs.getBoolean("handsFree", false)
    fun setHandsFree(enabled: Boolean) {
        prefs.edit().putBoolean("handsFree", enabled).apply()
        refreshBubble?.invoke(); publish(); continueAutomatic()
    }
    fun environmentChanged() { refreshBubble?.invoke(); publish(); continueAutomatic() }
    fun scanApps() {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val entries = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        installedApps = entries.map { InstalledApp(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }.sortedBy { it.name.lowercase(Locale.ROOT) }
        publish()
    }
    fun openPackage(packageName: String) {
        if (pendingId != null) { say("Cancel or finish the current action first."); return }
        scanApps()
        val app = installedApps.firstOrNull { it.packageName == packageName }
            ?: run { fail("Open app", "That app is no longer available."); return }
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent == null) { fail("Open app", "That app is no longer available."); return }
        request("Open ${app.name}", "Open ${app.name}", "I'll open ${app.name} once your phone is unlocked.", "launch", intent, "Opening ${app.name}, boss.")
    }

    fun submit(input: String) {
        val text = input.trim()
        if (text.isEmpty()) return
        when (EmergencyPhraseStore.match(context, text)) {
            EmergencyPhraseMatch.START -> {
                transcript = ""
                if (!EmergencyPhraseStore.enabled(context)) {
                    say("Emergency protection is not enabled. Finish setup in Settings first.", "Emergency setup is incomplete, boss.")
                } else if (EmergencyPhraseStore.active(context)) {
                    say("Emergency location sharing is already active, boss.")
                } else {
                    context.startForegroundService(Intent(context, EmergencyLocationService::class.java).setAction(EmergencyLocationService.ACTION_START))
                    record("Emergency alert", "Trusted-contact location sharing started.")
                    say("Emergency alert activated. I am sharing location updates with your trusted contact.", "Emergency alert activated, boss.")
                }
                return
            }
            EmergencyPhraseMatch.REMOTE -> {
                transcript = ""
                when {
                    !EmergencyPhraseStore.enabled(context) -> say("Emergency protection is not enabled. Finish setup in Settings first.", "Emergency setup is incomplete, boss.")
                    !GhostLink.connected -> say("Connect Ghost Link first. I cannot send the emergency alert through the other phone.", "Ghost Link is not connected, boss.")
                    else -> {
                        context.startForegroundService(Intent(context, EmergencyLocationService::class.java).setAction(EmergencyLocationService.ACTION_REMOTE))
                        record("Remote emergency alert", "Getting a fresh location for an SMS from the linked phone.")
                        say("I am sending the emergency alert through your linked phone.", "Sending the linked-phone alert, boss.")
                    }
                }
                return
            }
            EmergencyPhraseMatch.STOP -> {
                transcript = ""
                context.startService(Intent(context, EmergencyLocationService::class.java).setAction(EmergencyLocationService.ACTION_STOP))
                record("Emergency alert", "Location sharing stopped.")
                say("Emergency location sharing stopped, boss.")
                return
            }
            EmergencyPhraseMatch.NONE -> Unit
        }
        transcript = text
        val remote = RemoteCommand.extract(text)
        if (remote != null) {
            if (pendingId != null || contactChoices.isNotEmpty() || waitingForAlarmTime || WhatsAppAutomation.sender?.busy == true) {
                fail(text, "Finish or cancel the current action before controlling the other phone.")
                return
            }
            when (val action = CommandParser.parse(remote)) {
                is GhostCommand.OpenApp -> try {
                    GhostLink.openApp(action.name)
                    record(text, "Sent app request to the other phone; waiting for its result.")
                } catch (e: Exception) { fail(text, e.message ?: "Ghost Link is unavailable.") }
                else -> fail(text, "Ghost Link currently supports opening apps. Try open Chrome on my other phone.")
            }
            return
        }
        val parsed = resolveContext(text) ?: CommandParser.parse(text)
        if (pendingId != null) {
            val answer = text.lowercase(Locale.ROOT).trimEnd('.', '!', '?')
            if (answer in listOf("yes", "yeah", "confirm", "do it", "continue")) {
                if (locked()) say("Unlock your phone first, then say yes again.", "Unlock first, boss.")
                else runCatching { approve(pendingId ?: "") }.onFailure { fail(text, it.message ?: "I couldn't continue that action.") }
                return
            }
            if (answer in listOf("no", "nope", "cancel it", "don't", "do not")) { cancel(); return }
        }
        val command = if (waitingForAlarmTime && parsed is GhostCommand.Unknown)
            CommandParser.parse("set an alarm for $text") else parsed
        waitingForAlarmTime = false
        if (contactChoices.isNotEmpty() && command !is GhostCommand.Cancel) {
            if (SystemClock.elapsedRealtime() > contactDeadline) {
                contactChoices = emptyList(); contactMessage = null
                fail(text, "That contact choice expired. Please give the message command again.")
                return
            }
            val chosen = ContactMatcher.choose(text.replace(Regex("^hey ghosts? ", RegexOption.IGNORE_CASE), ""), contactChoices)
            if (chosen == null) ask("Which one, boss? Say the full name, or first or second.")
            else chooseContact(chosen.id)
            return
        }
        if (command is GhostCommand.Cancel) { cancel(); stopListening?.invoke(); return }
        if (WhatsAppAutomation.sender?.busy == true) {
            say("I'm checking your WhatsApp message. Say cancel to stop it before giving another command.")
            return
        }
        if (command is GhostCommand.Unlock) { requestUnlock(); return }
        if (command is GhostCommand.ScanApps) {
            scanApps()
            record(text, "Found ${installedApps.size} launchable apps. Browse them in Settings > Installed apps.")
            say("I found ${installedApps.size} launchable apps. You can say open followed by an app name.")
            return
        }
        if (pendingId != null) {
            say("Review or cancel the current action before starting another.")
            return
        }
        when (command) {
            is GhostCommand.OpenApp -> {
                scanApps()
                when (val match = AppMatcher.match(command.name, installedApps)) {
                    is AppMatch.Found -> { lastAppName = match.app.name; openPackage(match.app.packageName) }
                    is AppMatch.Ambiguous -> fail(text, "Several apps match: ${match.apps.joinToString { it.name }}. Say the full app name or choose it in Settings > Installed apps.")
                    AppMatch.Missing -> fail(text, "No launchable app matched ${command.name}. Check Installed apps in Settings for its exact name.")
                }
            }
            is GhostCommand.Message -> resolveMessage(text, command)
            is GhostCommand.Call -> resolveCall(text, command.recipient)
            is GhostCommand.CallControl -> {
                val outcome = try { CallActions.execute(context, command.action) }
                    catch (e: Exception) { fail(text, e.message ?: "Android could not control the call."); return }
                record(text, outcome); say(outcome)
            }
            is GhostCommand.Media -> {
                val outcome = MediaActions.execute(context, command.action)
                record(text, outcome); say(outcome, outcome.removeSuffix("."))
            }
            GhostCommand.ReadNotifications -> readNotifications(text)
            is GhostCommand.NotificationReply -> prepareNotificationReply(text, command)
            is GhostCommand.DrivingMode -> {
                setDrivingMode(command.enabled)
                val outcome = if (command.enabled) "Driving mode enabled. Replies will be brief and Ghost will prefer a connected Bluetooth headset."
                    else "Driving mode disabled."
                record(text, outcome); say(outcome, if (command.enabled) "Driving mode on, boss." else "Driving mode off, boss.")
            }
            is GhostCommand.CreateRoutine -> createRoutine(text, command)
            is GhostCommand.RunRoutine -> runRoutine(text, command.name)
            is GhostCommand.DeleteRoutine -> deleteRoutineByName(text, command.name)
            GhostCommand.ListRoutines -> listRoutines(text)
            is GhostCommand.TaskControl -> controlTask(text, command.action)
            is GhostCommand.Alarm -> {
                val label = "${(command.hour + 11) % 12 + 1}${if (command.minute == 0) "" else ":%02d".format(command.minute)} ${if (command.hour < 12) "AM" else "PM"}"
                val intent = Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, command.hour).putExtra(AlarmClock.EXTRA_MINUTES, command.minute)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, "Ghost alarm").putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                request(text, "Alarm for $label", "I'll send this to your clock when your phone is unlocked. Deferred requests expire after 10 minutes.", "alarm", intent, "I've asked your clock to set an alarm for $label, boss. Check the clock to confirm it's saved.")
            }
            is GhostCommand.Reminder -> {
                if (locked()) fail(text, "Unlock your phone first, then give the reminder command again.")
                else {
                    val reminder = ReminderStore(context).add(command.text, command.triggerAt)
                    val whenText = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(reminder.triggerAt))
                    record(text, "Reminder scheduled for $whenText: ${reminder.text}")
                    say("I'll remind you to ${reminder.text} on $whenText, boss.", "Reminder set for $whenText, boss.")
                }
            }
            is GhostCommand.ListReminders -> {
                val reminders = ReminderStore(context).all().filter {
                    (command.from == null || it.triggerAt >= command.from) && (command.until == null || it.triggerAt < command.until)
                }
                if (reminders.isEmpty()) {
                    record(text, "No reminders are pending for ${command.label}.")
                    say("You have no reminders pending for ${command.label}, boss.")
                } else {
                    val details = reminders.take(8).joinToString(". ") { reminder ->
                        "${reminder.text} at ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(reminder.triggerAt))}"
                    }
                    val extra = if (reminders.size > 8) ". And ${reminders.size - 8} more in Settings" else ""
                    record(text, "$details$extra")
                    say("You have ${reminders.size} pending. $details$extra", "You have ${reminders.size} pending reminders, boss. $details$extra")
                }
            }
            is GhostCommand.DeleteReminder -> deleteReminderByCommand(text, command.query)
            GhostCommand.DismissAlarm -> {
                val intent = Intent(AlarmClock.ACTION_DISMISS_ALARM)
                    .putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_NEXT)
                request(text, "Turn off alarm", "I'll ask your clock to dismiss the next active alarm when your phone is unlocked.", "alarm", intent, "I've asked your clock to dismiss the alarm, boss.")
            }
            GhostCommand.Help -> {
                val help = "Try open followed by an installed app name, scan apps, unlock phone, message Harsh on WhatsApp with hi, or set an alarm for 6 am. Allow phone contacts in Settings to message by name."
                record(text, help); say(help, "I can open installed apps, request unlock, prepare drafts, and request alarms.")
            }
            GhostCommand.AlarmTimeNeeded -> {
                waitingForAlarmTime = true
                record(text, "Sure, boss. What time?")
                ask("Sure, boss. What time?")
            }
            is GhostCommand.Chat -> { record(text, command.reply); say(command.reply, followUp = true) }
            is GhostCommand.Unknown -> {
                if (command.reason.contains("alarm request") || command.reason.contains("AM or PM")) waitingForAlarmTime = true
                val friendly = FriendlyConversation.reply(text)
                if (friendly != null) {
                    record(text, friendly); say(friendly, followUp = true)
                } else if (command.reason.startsWith("I heard") && OfflineAi.respond(
                        text,
                        prefs.getString("voiceMode", "brief") == "full"
                    ) { result ->
                        val reply = result.getOrElse { "My offline AI had trouble answering, boss. Try again in a moment." }
                        record(text, reply); say(reply, followUp = true)
                    }) {
                    status = "Thinking offline…"; transcript = ""; publish()
                } else {
                    record(text, command.reason)
                    ask(command.reason)
                }
            }
            GhostCommand.Cancel -> Unit
            GhostCommand.Unlock, GhostCommand.ScanApps -> Unit
        }
    }

    private fun resolveContext(text: String): GhostCommand? {
        val simple = text.lowercase(Locale.ROOT).trim().trimEnd('.', '!', '?')
        if (simple in listOf("open it again", "open that again", "open the last app"))
            return lastAppName?.let(GhostCommand::OpenApp)
        Regex("^(?:message|text) (?:the )?same person (?:with|saying) (.+)$", RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            return lastContactName?.let { name -> GhostCommand.Message(name, it.groupValues[1].trim()) }
        }
        if (simple in listOf("call the same person", "call them again")) return lastContactName?.let(GhostCommand::Call)
        if (simple in listOf("delete the last reminder", "remove the last reminder")) {
            return ReminderStore(context).all().maxByOrNull { it.id }?.let { GhostCommand.DeleteReminder(it.text) }
        }
        return null
    }

    private fun createRoutine(spoken: String, command: GhostCommand.CreateRoutine) {
        val routine = try { RoutineStore(context).save(command.name, command.steps) }
            catch (e: IllegalArgumentException) { fail(spoken, e.message ?: "I couldn't save that routine."); return }
        val outcome = "Saved ${routine.name} with ${routine.steps.size} steps."
        record(spoken, outcome); say("$outcome Say run ${routine.name} routine whenever you need it.", outcome)
    }

    private fun listRoutines(spoken: String) {
        val routines = RoutineStore(context).all()
        val outcome = if (routines.isEmpty()) "You have no saved routines." else "Your routines are ${routines.joinToString { it.name }}."
        record(spoken, outcome); say(outcome)
    }

    fun runRoutine(spoken: String, name: String = spoken) {
        val routine = RoutineStore(context).find(name) ?: run { fail(spoken, "I couldn't find a routine named $name."); return }
        if (locked()) { fail(spoken, "Unlock your phone first, then run ${routine.name} again."); return }
        if (activeTask != null) { fail(spoken, "Finish or cancel the current routine first."); return }
        record(spoken, "Running ${routine.name}: ${routine.steps.joinToString("; ")}")
        activeTask = RoutineTask(routine.name, routine.steps)
        publish(); runRoutineStep()
    }

    private fun runRoutineStep() {
        val task = activeTask ?: return
        if (task.index >= task.steps.size) {
            activeTask = null; publish()
            say("${task.name} routine finished. All ${task.steps.size} steps were dispatched successfully, boss.", "${task.name} finished.")
            return
        }
        task.state = "running"; task.error = null; publish()
        val step = task.steps[task.index]
        val command = CommandParser.parse(step)
        val result = runCatching { when (command) {
            is GhostCommand.Media -> MediaActions.execute(context, command.action)
            is GhostCommand.DrivingMode -> { setDrivingMode(command.enabled); "Driving mode changed." }
            is GhostCommand.OpenApp -> {
                scanApps()
                val match = AppMatcher.match(command.name, installedApps)
                if (match is AppMatch.Found) {
                    val intent = context.packageManager.getLaunchIntentForPackage(match.app.packageName)
                        ?: error("${match.app.name} no longer has a launch screen")
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    lastAppName = match.app.name
                    "Opened ${match.app.name}."
                } else error("I couldn't uniquely match ${command.name}")
            }
            else -> error("That step is no longer supported")
        } }
        if (result.isFailure) {
            task.state = "paused"; task.error = result.exceptionOrNull()?.message ?: "The step failed"
            publish(); say("${task.name} paused at step ${task.index + 1}: ${task.error}. Say retry step, skip step, or cancel.", "Routine paused. Retry, skip, or cancel.", followUp = true)
            return
        }
        task.results.add(result.getOrThrow()); task.index++ ; publish()
        Handler(Looper.getMainLooper()).postDelayed(::runRoutineStep, 450)
    }

    private fun controlTask(spoken: String, action: String) {
        val task = activeTask ?: run { fail(spoken, "There is no paused routine, boss."); return }
        if (task.state != "paused") { fail(spoken, "${task.name} is still running."); return }
        when (action) {
            "retry", "continue" -> runRoutineStep()
            "skip" -> { task.results.add("Skipped: ${task.steps[task.index]}"); task.index++; runRoutineStep() }
        }
    }

    fun deleteRoutine(name: String) { RoutineStore(context).delete(name); publish() }
    private fun deleteRoutineByName(spoken: String, name: String) {
        if (RoutineStore(context).delete(name)) { record(spoken, "Deleted $name routine."); say("Deleted the $name routine, boss.") }
        else fail(spoken, "I couldn't find a routine named $name.")
    }

    private fun resolveCall(spoken: String, recipient: String) {
        if (!PhoneContacts.allowed(context)) { fail(spoken, "Allow contacts in Settings first, boss."); return }
        if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            fail(spoken, "Allow phone calls in Settings before I can call a contact, boss."); return
        }
        val matches = ContactMatcher.match(recipient, PhoneContacts.read(context))
        when (matches.size) {
            0 -> fail(spoken, "I couldn't find $recipient in your phone contacts.")
            1 -> {
                val contact = matches.single()
                lastContactName = contact.name
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contact.phone}"))
                request(spoken, "Call ${contact.name}?", "Phone number ending ${contact.phone.takeLast(4)}. Voice confirmation is required before calling.", "call", intent, "Calling ${contact.name}, boss.")
            }
            else -> {
                val names = matches.take(5).joinToString(", ") { it.name }
                record(spoken, "Several contacts match: $names")
                ask("I found several matches: $names. Say call followed by the full name.")
            }
        }
    }

    private fun readNotifications(spoken: String) {
        if (!notificationReading()) { fail(spoken, "Notification reading is off. Enable it in Ghost Settings first."); return }
        if (!NotificationCenter.accessGranted(context)) { fail(spoken, "Enable Ghost notification access in Android Settings first."); return }
        if (locked()) { fail(spoken, "Unlock your phone first so I can read private notifications."); return }
        val items = NotificationCenter.latest(5)
        if (items.isEmpty()) { record(spoken, "No recent notifications."); say("You have no recent notifications, boss."); return }
        val summary = items.joinToString(". ") { item -> "From ${item.app}: ${item.title}${if (item.text.isBlank()) "" else ", ${item.text}"}" }
        record(spoken, summary); say("You have ${items.size} recent notifications. $summary", "You have ${items.size} recent notifications, boss. $summary")
    }

    private fun prepareNotificationReply(spoken: String, command: GhostCommand.NotificationReply) {
        if (!notificationReading()) { fail(spoken, "Notification replies are off. Enable notification reading in Ghost Settings first."); return }
        if (!NotificationCenter.accessGranted(context)) { fail(spoken, "Enable Ghost notification access in Android Settings first."); return }
        if (locked()) { fail(spoken, "Unlock your phone first, then give the reply command again."); return }
        val matches = NotificationCenter.replyMatches(command.target)
        when {
            matches.isEmpty() -> fail(spoken, if (command.target.isBlank()) "I couldn't find a recent notification that supports replies." else "I couldn't find a replyable notification matching ${command.target}.")
            command.target.isNotBlank() && matches.size > 1 && matches[0].title != matches[1].title -> {
                val choices = matches.take(4).joinToString(", ") { "${it.title} in ${it.app}" }
                record(spoken, "Several replyable notifications match: $choices")
                ask("I found several matches: $choices. Say reply to followed by the exact name and message.")
            }
            else -> {
                val notification = matches.first()
                val endpoint = notification.reply ?: run { fail(spoken, "That notification no longer supports replies."); return }
                requestOperation(
                    spoken,
                    "Reply to ${notification.title}?",
                    "Through ${notification.app}. Message: ${command.message}",
                    "notificationReply",
                    "Reply sent to ${notification.title} through ${notification.app}.",
                ) { endpoint.send(context, command.message) }
            }
        }
    }

    private fun resolveMessage(text: String, command: GhostCommand.Message) {
        if (!PhoneContacts.allowed(context)) { fail(text, "Allow contacts in Settings > WhatsApp contacts, boss. Then I can find people by name."); return }
        val matches = try { ContactMatcher.match(command.recipient, PhoneContacts.read(context)) }
            catch (_: Exception) { fail(text, "I couldn't read your phone contacts. Check the contacts permission."); return }
        if (matches.isEmpty()) { fail(text, "I couldn't find ${command.recipient} with a usable phone number. Use the full name saved in your phone contacts."); return }
        if (matches.size == 1) { prepareMessage(matches.single(), command.body); return }
        contactChoices = matches; contactMessage = command
        contactDeadline = SystemClock.elapsedRealtime() + 120000
        Handler(Looper.getMainLooper()).postDelayed({
            if (contactMessage === command) { contactChoices = emptyList(); contactMessage = null; publish() }
        }, 120000)
        val names = contactChoices.take(4).mapIndexed { i, c -> "${i + 1}: ${c.name}, number ending ${c.phone.takeLast(4)}" }.joinToString(". ")
        record(text, "Several contacts match. $names")
        ask("I found more than one, boss. $names. Which one?")
    }
    fun chooseContact(id: String) {
        require(SystemClock.elapsedRealtime() <= contactDeadline) { "Contact selection expired; give the command again." }
        val chosen = contactChoices.firstOrNull { it.id == id } ?: error("That contact is no longer a choice.")
        val body = contactMessage?.body ?: error("No message is pending.")
        contactChoices = emptyList(); contactMessage = null; publish()
        prepareMessage(chosen, body)
    }
    private fun prepareMessage(contact: PhoneContact, body: String) {
        lastContactName = contact.name
        val uri = Uri.Builder().scheme("https").authority("wa.me").appendPath(contact.phone.removePrefix("+"))
            .appendQueryParameter("text", body).build()
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp")
        if (WhatsAppAutomation.available && prefs.getBoolean("autoSend", false) && WhatsAppAutomation.sender != null) {
            messagePlan = contact; messageBody = body
            request("Message ${contact.name}", "WhatsApp to ${contact.name}", "Message: $body. Unlock and open Ghost to continue.",
                "whatsapp", intent, "Opening ${contact.name}'s chat. I'll check the message before tapping Send.")
        } else {
            request("Message ${contact.name}", "Prepare WhatsApp draft", "To: ${contact.name}\nNumber: ${contact.phone}\nMessage: $body\n\nOpen this draft and press Send in WhatsApp. You press Send in WhatsApp.",
                "draft", intent, "Your WhatsApp draft for ${contact.name} is ready. Press Send in WhatsApp, boss.")
        }
    }
    fun messageAutomationResult(message: String) { record("WhatsApp", message); say(message) }

    private fun locked() = context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
    private fun canLaunch() = activeHost() != null ||
        (handsFreeEnabled() && Settings.canDrawOverlays(context) && bubbleVisible?.invoke() == true)
    private fun activeHost(): Activity? {
        val activity = host.get() ?: return null
        if (activity.isFinishing || activity.isDestroyed || context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) return null
        return activity
    }
    private fun request(command: String, title: String, detail: String, kind: String, intent: Intent, outcome: String) {
        val disposition = ActionPolicy.decide(kind, locked(), canLaunch())
        if (disposition == ActionDisposition.EXECUTE) {
            launch(intent, outcome)
            return
        }
        pendingId = UUID.randomUUID().toString(); pendingTitle = title; pendingDetail = detail
        pendingKind = kind; pendingIntent = intent; pendingOperation = null; pendingResult = outcome
        val automatic = disposition != ActionDisposition.REVIEW
        pendingDeadline = if (automatic) SystemClock.elapsedRealtime() + (if (kind == "whatsapp") 120000 else 600000)
            else if (kind == "call") SystemClock.elapsedRealtime() + 120000 else 0L
        val id = pendingId
        if (automatic) Handler(Looper.getMainLooper()).postDelayed({ if (id == pendingId) continueAutomatic() }, if (kind == "whatsapp") 120000 else 600000)
        record(command, if (automatic) "Waiting to continue: $title" else "Awaiting your review: $title")
        if (activeHost() == null) notifyAction()
        if (!automatic) say("$title. $detail Say yes to confirm or no to cancel.", "$title Say yes or no.", followUp = true)
        else if (locked()) say("Unlock your phone first. I will continue when launching is allowed.", "Waiting for unlock, boss.")
        else say("Tap the Ghost notification to continue, or enable hands-free launches in Settings.", "Tap Ghost to continue.")
    }
    private fun requestOperation(command: String, title: String, detail: String, kind: String, outcome: String, operation: () -> Unit) {
        check(ActionPolicy.requiresReview(kind)) { "Background operations must require review." }
        pendingId = UUID.randomUUID().toString(); pendingTitle = title; pendingDetail = detail
        pendingKind = kind; pendingIntent = null; pendingOperation = operation; pendingResult = outcome
        pendingDeadline = SystemClock.elapsedRealtime() + 120000
        record(command, "Awaiting your review: $title")
        if (activeHost() == null) notifyAction()
        say("$title. $detail. Say yes to send it or no to cancel.", "$title Say yes or no.", followUp = true)
        publish()
    }
    private fun notifyAction() {
        val open = PendingIntent.getActivity(context, 20, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, "ghost_actions").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (pendingKind?.let(ActionPolicy::requiresReview) == true) "Ghost needs your review" else "Ghost is waiting to continue").setContentText("Unlock and tap to continue.")
            .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build()
        context.getSystemService(NotificationManager::class.java).notify(20, notification)
    }
    fun approve(id: String) {
        require(id == pendingId) { "This action is no longer pending." }
        require(pendingDeadline == 0L || SystemClock.elapsedRealtime() < pendingDeadline) { "This request expired. Cancel it and give the command again." }
        require(!locked()) { "Unlock your phone to continue." }
        val operation = pendingOperation
        if (operation == null) require(canLaunch()) { "Open Ghost or enable its bubble to continue." }
        val intent = pendingIntent
        val result = pendingResult ?: "Request delivered."
        clearPending()
        if (operation != null) {
            try { operation(); record("Confirmed action", result); say(result) }
            catch (e: Exception) { fail("Confirmed action", "The reply could not be sent. ${e.localizedMessage ?: "The notification may have expired."}") }
        } else if (intent != null) launch(intent, result)
    }
    fun continueAutomatic() {
        if (pendingId == null || pendingKind?.let(ActionPolicy::requiresReview) == true) return
        if (SystemClock.elapsedRealtime() >= pendingDeadline) {
            messagePlan = null; clearPending(); record("Deferred action", "Request expired. Give the command again."); return
        }
        if (locked() || !canLaunch()) return
        val intent = pendingIntent ?: return
        val result = pendingResult ?: "Request delivered."
        clearPending(); launch(intent, result)
    }

    fun configureEmergency(phrase: String, remotePhrase: String, stopPhrase: String, phone: String, message: String) {
        EmergencyPhraseStore.configure(context, phrase, remotePhrase, stopPhrase, phone, message)
        publish()
    }
    fun setEmergencyEnabled(enabled: Boolean) { EmergencyPhraseStore.setEnabled(context, enabled); publish() }
    fun stopEmergency() {
        context.startService(Intent(context, EmergencyLocationService::class.java).setAction(EmergencyLocationService.ACTION_STOP))
    }
    fun clearEmergency() {
        stopEmergency(); EmergencyPhraseStore.clear(context); publish()
    }
    fun deleteReminder(id: Long) { ReminderStore(context).delete(id); publish() }
    private fun deleteReminderByCommand(spokenCommand: String, query: String) {
        val normalized = query.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9: ]"), " ").replace(Regex("\\s+"), " ").trim()
        val tokens = normalized.split(" ").filter { (it.length > 1 || it.all(Char::isDigit)) && it !in setOf("the", "reminder", "please", "my") }
        val clock = java.text.SimpleDateFormat("h:mm a HH:mm d MMMM yyyy", Locale.ENGLISH)
        val matches = ReminderStore(context).all().filter { reminder ->
            val searchable = "${reminder.text} ${clock.format(Date(reminder.triggerAt))}".lowercase(Locale.ROOT)
            tokens.isNotEmpty() && tokens.all { searchable.contains(it) }
        }
        when (matches.size) {
            0 -> fail(spokenCommand, "I couldn't find a pending reminder matching $query, boss.")
            1 -> {
                val reminder = matches.single(); ReminderStore(context).delete(reminder.id)
                record(spokenCommand, "Deleted reminder: ${reminder.text}")
                say("Deleted the reminder to ${reminder.text}, boss.")
            }
            else -> {
                val choices = matches.take(5).joinToString(". ") { "${it.text} at ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it.triggerAt))}" }
                record(spokenCommand, "Several reminders matched: $choices")
                ask("I found ${matches.size} matching reminders. $choices. Say delete reminder followed by the exact subject or time.")
            }
        }
    }
    fun reminderDue(text: String) {
        Handler(Looper.getMainLooper()).post {
            record("Reminder", text)
            say("Boss, this is your reminder: $text", "Reminder, boss: $text")
        }
    }
    fun emergencyChanged() { Handler(Looper.getMainLooper()).post { publish(); prepareEmergencyWhatsApp() } }
    private fun prepareEmergencyWhatsApp() {
        if (locked() || activeHost() == null || pendingId != null) return
        val text = EmergencyPhraseStore.takeWhatsAppPending(context) ?: return
        val phone = EmergencyPhraseStore.phone(context).removePrefix("+")
        val intent = Intent(Intent.ACTION_VIEW, Uri.Builder().scheme("https").authority("wa.me").appendPath(phone).appendQueryParameter("text", text).build()).setPackage("com.whatsapp")
        if (intent.resolveActivity(context.packageManager) != null) {
            request("Emergency alert", "Emergency WhatsApp draft", "Location update for your trusted contact. Review it and press Send.", "draft", intent, "Your emergency WhatsApp draft is ready. Press Send, boss.")
        }
    }
    private fun requestUnlock() {
        if (!locked()) { record("Unlock phone", "Your phone is already unlocked."); say("Your phone is already unlocked, boss."); continueAutomatic(); return }
        val intent = Intent(context, UnlockActivity::class.java)
        if (host.get() != null) {
            host.get()?.startActivity(intent)
        } else {
            val open = PendingIntent.getActivity(context, 21, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            context.getSystemService(NotificationManager::class.java).notify(21,
                Notification.Builder(context, "ghost_actions").setSmallIcon(android.R.drawable.ic_lock_lock)
                    .setContentTitle("Unlock with Android").setContentText("Tap to open the phone's authentication prompt.")
                    .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build())
        }
        record("Unlock phone", "Android authentication requested. Tap the unlock notification if Ghost is in the background.")
        say("Use Android authentication to unlock. Tap my unlock notification to open the prompt.", "Tap my notification to unlock, boss.")
    }
    fun unlockResult(message: String) {
        context.getSystemService(NotificationManager::class.java).cancel(21)
        record("Unlock phone", message)
    }
    private fun launch(intent: Intent, outcome: String) {
        try {
            check(!locked() && canLaunch()) { "Open Ghost and unlock your phone to continue." }
            val activity = activeHost()
            if (intent.data?.host == "wa.me" && messagePlan != null) {
                val planned = messagePlan!!
                messagePlan = null
                val contacts = PhoneContacts.read(context)
                check(contacts.any { it.name == planned.name && it.phone == planned.phone }) { "This contact changed. Give the message command again." }
                val automation = WhatsAppAutomation.sender ?: error("Enable Ghost WhatsApp Send in Android accessibility settings.")
                automation.arm(planned.name, planned.phone, messageBody,
                    contacts.filter { it.name.equals(planned.name, true) }.map { it.phone }.distinct().size == 1)
            }
            if (activity != null) activity.startActivity(intent)
            else context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            if (intent.action == AlarmClock.ACTION_SET_ALARM && !locked()) {
                val show = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                if (show.resolveActivity(context.packageManager) != null) {
                    try {
                        if (activity != null) activity.startActivity(show)
                        else context.startActivity(show.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: Exception) { /* Alarm request already sent; showing its list is optional. */ }
                }
            }
            record("Android action", outcome)
            say(outcome, if (intent.action == AlarmClock.ACTION_SET_ALARM) "On it, boss. I've sent the alarm to your clock." else "Opening it, boss.")
        } catch (e: Exception) { WhatsAppAutomation.sender?.cancel(); messagePlan = null; fail("Android action", "Could not open the requested screen. ${e.localizedMessage ?: "The app may not be installed."}") }
    }
    private fun clearPending() {
        pendingIntent = null; pendingOperation = null; pendingId = null; pendingTitle = null; pendingDetail = null; pendingKind = null; pendingResult = null; pendingDeadline = 0L
        context.getSystemService(NotificationManager::class.java).cancel(20)
        publish()
    }
    fun cancel() {
        waitingForAlarmTime = false
        clarification = null
        contactChoices = emptyList(); contactMessage = null; messagePlan = null
        WhatsAppAutomation.sender?.cancel()
        activeTask = null
        clearPending()
        context.getSystemService(NotificationManager::class.java).cancel(21)
        endInterruptionListening?.invoke()
        speech.stop()
        resumeRecognition?.invoke()
        record("Cancel", "Pending action cancelled.")
    }
    fun silence() { endInterruptionListening?.invoke(); speech.stop() }
    private fun fail(command: String, message: String) { record(command, message); say(message) }
}

private data class RoutineTask(
    val name: String,
    val steps: List<String>,
    var index: Int = 0,
    var state: String = "running",
    var error: String? = null,
    val results: MutableList<String> = mutableListOf(),
) {
    fun snapshot() = mapOf(
        "name" to name, "steps" to steps, "index" to index, "state" to state,
        "error" to error, "results" to results.toList(),
    )
}
