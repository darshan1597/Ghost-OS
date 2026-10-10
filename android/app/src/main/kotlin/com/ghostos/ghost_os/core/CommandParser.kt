package com.ghostos.ghost_os.core

sealed class GhostCommand {
    data class OpenApp(val name: String) : GhostCommand()
    data class Message(val recipient: String, val body: String) : GhostCommand()
    data class Alarm(val hour: Int, val minute: Int) : GhostCommand()
    data class Reminder(val text: String, val triggerAt: Long) : GhostCommand()
    data class ListReminders(val from: Long?, val until: Long?, val label: String) : GhostCommand()
    data class DeleteReminder(val query: String) : GhostCommand()
    data class Call(val recipient: String) : GhostCommand()
    data class CallControl(val action: String) : GhostCommand()
    data class Media(val action: String) : GhostCommand()
    data object ReadNotifications : GhostCommand()
    data class NotificationReply(val target: String, val message: String) : GhostCommand()
    data class CreateRoutine(val name: String, val steps: List<String>) : GhostCommand()
    data class RunRoutine(val name: String) : GhostCommand()
    data class DeleteRoutine(val name: String) : GhostCommand()
    data class DrivingMode(val enabled: Boolean) : GhostCommand()
    data class TaskControl(val action: String) : GhostCommand()
    data object ListRoutines : GhostCommand()
    data class Chat(val reply: String) : GhostCommand()
    data object AlarmTimeNeeded : GhostCommand()
    data object Cancel : GhostCommand()
    data object Help : GhostCommand()
    data object Unlock : GhostCommand()
    data object ScanApps : GhostCommand()
    data object DismissAlarm : GhostCommand()
    data class Unknown(val reason: String) : GhostCommand()
}

/** Accept natural variants locally, but never invent a recipient, message or time. */
object CommandParser {
    fun parse(input: String): GhostCommand {
        var text = input.trim().replace(Regex("\\s+"), " ")
        repeat(3) {
            text = text.replace(Regex("^(?:hey ghost|ghost|please|can you|could you|would you|i want you to)[,.]?\\s+", RegexOption.IGNORE_CASE), "")
        }
        val simple = text.lowercase(java.util.Locale.ROOT).trimEnd('.', '!', '?')
        if (simple in listOf("cancel", "stop", "stop listening", "never mind", "nevermind")) return GhostCommand.Cancel
        if (simple in listOf("continue task", "continue routine", "continue")) return GhostCommand.TaskControl("continue")
        if (simple in listOf("retry step", "retry that", "try again")) return GhostCommand.TaskControl("retry")
        if (simple in listOf("skip step", "skip that", "skip")) return GhostCommand.TaskControl("skip")
        if (simple in listOf("help", "what can you do")) return GhostCommand.Help
        if (simple in listOf("unlock phone", "unlock my phone", "unlock the phone", "unlock device")) return GhostCommand.Unlock
        if (simple in listOf("scan apps", "scan my apps", "list apps", "refresh apps", "what apps are installed")) return GhostCommand.ScanApps
        if (simple in listOf("stop alarm", "stop the alarm", "turn off alarm", "turn off the alarm", "dismiss alarm", "dismiss the alarm", "switch off alarm", "switch off the alarm")) return GhostCommand.DismissAlarm
        if (simple in listOf("read notifications", "read my notifications", "what notifications did i miss", "what are my notifications")) return GhostCommand.ReadNotifications
        if (simple in listOf("start driving mode", "enable driving mode", "turn on driving mode", "i am driving")) return GhostCommand.DrivingMode(true)
        if (simple in listOf("stop driving mode", "disable driving mode", "turn off driving mode", "i stopped driving")) return GhostCommand.DrivingMode(false)
        if (simple in listOf("list routines", "what are my routines", "show my routines")) return GhostCommand.ListRoutines
        Regex("^(?:run|start|do) (?:my )?(.+?) routine$", RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            return GhostCommand.RunRoutine(it.groupValues[1].trim())
        }
        Regex("^(?:delete|remove) (?:my )?(.+?) routine$", RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            return GhostCommand.DeleteRoutine(it.groupValues[1].trim())
        }
        Regex("^(?:create|save|make) (?:a )?routine (?:called|named) (.+?) (?:with|to) (.+)$", RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            val steps = it.groupValues[2].split(Regex("\\s+(?:then|and then)\\s+", RegexOption.IGNORE_CASE)).map(String::trim)
            return GhostCommand.CreateRoutine(it.groupValues[1].trim(), steps)
        }
        Regex("^reply to (.+?) (?:with|saying) (.+)$", RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            return GhostCommand.NotificationReply(it.groupValues[1].trim(), it.groupValues[2].trim())
        }
        Regex("^reply (?:with|saying) (.+)$", RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            return GhostCommand.NotificationReply("", it.groupValues[1].trim())
        }
        if (simple in listOf("answer", "answer call", "answer the call", "pick up", "pick up the call")) return GhostCommand.CallControl("answer")
        if (simple in listOf("reject", "reject call", "reject the call", "decline call", "decline the call")) return GhostCommand.CallControl("reject")
        if (simple in listOf("end call", "end the call", "hang up", "hang up the call")) return GhostCommand.CallControl("end")
        when (simple) {
            "play", "play music", "resume", "resume music" -> return GhostCommand.Media("play")
            "pause", "pause music" -> return GhostCommand.Media("pause")
            "next", "next song", "next track", "skip song" -> return GhostCommand.Media("next")
            "previous", "previous song", "previous track", "go back a song" -> return GhostCommand.Media("previous")
            "volume up", "increase volume", "turn it up" -> return GhostCommand.Media("volumeUp")
            "volume down", "decrease volume", "turn it down" -> return GhostCommand.Media("volumeDown")
            "mute", "mute media" -> return GhostCommand.Media("mute")
            "unmute", "unmute media" -> return GhostCommand.Media("unmute")
        }
        Regex("^(?:call|phone|dial) (.+?)(?: for me)?[.!?]?$", RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            return GhostCommand.Call(it.groupValues[1].trim())
        }
        if (simple in listOf("hey", "hi", "hello", "hey ghost")) return GhostCommand.Chat("Hey boss! What would you like to do?")
        if (simple in listOf("thanks", "thank you", "thanks ghost")) return GhostCommand.Chat("Anytime, boss.")
        if (simple == "how are you") return GhostCommand.Chat("Ready to help, boss. What are we doing?")
        Regex("^(?:open|launch|start)(?: up)? (?:the )?(.+?)(?: app)?(?: for me)?[.!]?$", RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            return GhostCommand.OpenApp(it.groupValues[1].trim())
        }
        val patterns = listOf(
            "^(?:message|text) (.+?) on whats ?app (?:with|saying) (.+)$",
            "^send (?:a )?message to (.+?) on whats ?app (?:with|saying) (.+)$"
        )
        patterns.forEach { pattern -> Regex(pattern, RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            return GhostCommand.Message(it.groupValues[1].trim(), it.groupValues[2].trim())
        } }
        Regex("^send (.+?) to (.+?) on whats ?app$", RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            return GhostCommand.Message(it.groupValues[2].trim(), it.groupValues[1].trim())
        }
        val cleanText = text.trimEnd('.', '!', '?')
        Regex("^(?:what|which|list|show)(?: are| me)? (?:my )?(?:pending )?reminders?(?: (?:do i have|are pending))?(?: (?:for|on) (.+))?$", RegexOption.IGNORE_CASE).matchEntire(cleanText)?.let {
            val rawDate = it.groupValues[1].trim()
            if (rawDate.isEmpty()) return GhostCommand.ListReminders(null, null, "all pending reminders")
            val date = ReminderParser.parseDate(rawDate, java.time.LocalDate.now())
                ?: return GhostCommand.Unknown("I couldn't understand that reminder date, boss. Try today, tomorrow, or 15 October 2026.")
            val zone = java.time.ZoneId.systemDefault()
            return GhostCommand.ListReminders(date.atStartOfDay(zone).toInstant().toEpochMilli(), date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), rawDate)
        }
        Regex("^(?:delete|remove|cancel) (?:my )?(?:the )?reminder(?: (?:to|about|for|at|on))? (.+)$", RegexOption.IGNORE_CASE).matchEntire(cleanText)?.let {
            return GhostCommand.DeleteReminder(it.groupValues[1].trim())
        }
        ReminderParser.parse(text)?.let { return it }
        val alarm = Regex("^(?:(?:set|create|add|schedule) )?(?:(?:an?|the|my) )?(?:alarm|alaram|a larm)(?: (?:for|at|to))?(?: (.*))?$", RegexOption.IGNORE_CASE).matchEntire(text)
        if (alarm != null) {
            val raw = alarm.groupValues[1].trim()
            if (raw.isEmpty()) return GhostCommand.AlarmTimeNeeded
            val normalized = normalizeTime(raw)
            val match = Regex("^(\\d{1,2})(?::(\\d{2}))? ?(am|pm)?$").matchEntire(normalized)
                ?: return GhostCommand.Unknown("I caught the alarm request, boss, but not the time. Try six PM or six thirty PM.")
            var hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val period = match.groupValues[3]
            if (minute !in 0..59 || (period.isNotEmpty() && hour !in 1..12) || (period.isEmpty() && hour !in 0..23))
                return GhostCommand.Unknown("That time doesn't look right, boss. Try six PM.")
            if (period.isEmpty() && hour in 1..12) return GhostCommand.Unknown("Do you mean AM or PM, boss? Say the time again, like six PM.")
            if (period.isNotEmpty()) hour = hour % 12 + if (period == "pm") 12 else 0
            return GhostCommand.Alarm(hour, minute)
        }
        return GhostCommand.Unknown("I heard \"$text\", boss, but couldn't match that to an action. Could you rephrase it?")
    }

    internal fun normalizeTime(value: String): String {
        var normalized = value.lowercase(java.util.Locale.ROOT)
            .replace(Regex("\\b([ap])\\s*\\.?\\s*m\\b\\.?")) { "${it.groupValues[1]}m" }
            .replace(Regex("\\s+(?:in the evening|in the afternoon|tonight)$"), " pm")
            .replace(Regex("\\s+in the morning$"), " am")
            .replace(Regex("\\s+o['’]?clock\\b"), "")
            .replace(Regex("\\s+(?:please|for me)$"), "").trim().trimEnd('.', '!')
        // Preserve compact numeric times, e.g. 6pm, 6:30PM and 18:30.
        if (Regex("^\\d{1,2}(?::\\d{2})? ?(?:am|pm)?$").matches(normalized)) return normalized
        normalized = normalized.replace(Regex("(?<=\\p{L})(am|pm)$"), " $1")
        val words = mapOf("zero" to 0, "oh" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
            "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14, "fifteen" to 15,
            "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19,
            "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50)
        val tokens = normalized.split(" ").filter { it.isNotBlank() }
        val period = tokens.lastOrNull()?.takeIf { it == "am" || it == "pm" }
        val numbers = if (period != null) tokens.dropLast(1) else tokens
        if (numbers.isEmpty() || numbers.size > 3) return normalized
        val hour = words[numbers[0]] ?: numbers[0].toIntOrNull() ?: return normalized
        if (numbers.size == 1) return "$hour${period ?: ""}"
        val tens = words[numbers[1]] ?: numbers[1].toIntOrNull() ?: return normalized
        val units = if (numbers.size == 3) words[numbers[2]] ?: return normalized else 0
        if (numbers.size == 3 && (tens !in listOf(0, 20, 30, 40, 50) || units !in 0..9)) return normalized
        return "$hour:${(tens + units).toString().padStart(2, '0')}${period ?: ""}"
    }
}

object ReminderParser {
    private val command = Regex("^remind me(?: to| of| about)? (.+?) (?:at|on) (.+?)(?: on (.+)| (today|tomorrow))?$", RegexOption.IGNORE_CASE)
    fun parse(input: String, now: java.time.ZonedDateTime = java.time.ZonedDateTime.now()): GhostCommand.Reminder? {
        val match = command.matchEntire(input.trim()) ?: return null
        val subject = match.groupValues[1].trim()
        if (subject.isBlank()) return null
        val normalized = CommandParser.normalizeTime(match.groupValues[2].trim())
        val timeMatch = Regex("^(\\d{1,2})(?::(\\d{2}))? ?(am|pm)?$", RegexOption.IGNORE_CASE).matchEntire(normalized) ?: return null
        var hour = timeMatch.groupValues[1].toIntOrNull() ?: return null
        val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
        val period = timeMatch.groupValues[3].lowercase()
        if (minute !in 0..59 || period.isEmpty() && hour !in 0..23 || period.isNotEmpty() && hour !in 1..12) return null
        if (period.isNotEmpty()) hour = hour % 12 + if (period == "pm") 12 else 0
        val rawDate = (match.groupValues[3].ifBlank { match.groupValues[4] }).trim().lowercase()
        var date = parseDate(rawDate, now.toLocalDate()) ?: return null
        var result = java.time.ZonedDateTime.of(date, java.time.LocalTime.of(hour, minute), now.zone)
        if (rawDate.isEmpty() && !result.isAfter(now)) { date = date.plusDays(1); result = java.time.ZonedDateTime.of(date, java.time.LocalTime.of(hour, minute), now.zone) }
        if (!result.isAfter(now)) return null
        return GhostCommand.Reminder(subject, result.toInstant().toEpochMilli())
    }
    internal fun parseDate(value: String, today: java.time.LocalDate): java.time.LocalDate? {
        if (value.isEmpty() || value == "today") return today
        if (value == "tomorrow") return today.plusDays(1)
        val cleaned = value.removeSuffix(" date").trim()
        val formats = listOf("d MMMM uuuu", "d MMM uuuu", "MMMM d uuuu", "MMM d uuuu", "d/M/uuuu", "uuuu-M-d")
        for (pattern in formats) runCatching {
            val formatter = java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(java.util.Locale.ENGLISH)
            java.time.LocalDate.parse(cleaned, formatter)
        }.getOrNull()?.let { return it }
        for (pattern in listOf("d MMMM", "d MMM", "MMMM d", "MMM d")) {
            val parsed = runCatching {
                val formatter = java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(java.util.Locale.ENGLISH)
                java.time.MonthDay.parse(cleaned, formatter)
            }.getOrNull() ?: continue
            var candidate = parsed.atYear(today.year)
            if (candidate.isBefore(today)) candidate = parsed.atYear(today.year + 1)
            return candidate
        }
        return null
    }
}
