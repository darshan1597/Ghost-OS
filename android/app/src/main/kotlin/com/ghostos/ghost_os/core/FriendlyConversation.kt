package com.ghostos.ghost_os.core

import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Tiny deterministic conversation layer that is always available and needs no model. */
object FriendlyConversation {
    fun reply(input: String): String? {
        val text = input.lowercase(Locale.ROOT).trim().trimEnd('.', '!', '?')
        return when {
            Regex("^(who are you|what are you)$").matches(text) -> "I'm Ghost, your offline phone assistant. I'm here with you, boss."
            Regex(".*\b(sad|upset|lonely|bad day|not okay)\b.*").matches(text) -> "I'm with you, boss. Take one slow breath. Tell me what happened."
            Regex(".*\b(bored|boring)\b.*").matches(text) -> "Let's break the loop, boss. Put on one song, take a short walk, or give me a phone task."
            Regex(".*\b(tired|exhausted)\b.*").matches(text) -> "You've done enough for this moment, boss. Drink some water and give yourself a short reset."
            Regex(".*\b(motivate|motivation|give up)\b.*").matches(text) -> "One small move is enough, boss. Start for five minutes; momentum can handle the rest."
            Regex("^(tell me a joke|joke)$").matches(text) -> "Why did the phone need glasses? It lost all its contacts."
            Regex("^(what time is it|tell me the time|current time)$").matches(text) -> "It is ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())}, boss."
            Regex("^(what(?:'s| is) the date|today(?:'s| is) date|what day is it)$").matches(text) -> "Today is ${DateFormat.getDateInstance(DateFormat.FULL).format(Date())}, boss."
            Regex("^(good morning|good afternoon|good evening|good night)$").matches(text) -> "${text.replaceFirstChar { it.titlecase(Locale.ROOT) }}, boss. I'm ready when you are."
            Regex(".*\b(love you|you are awesome|you're awesome)\b.*").matches(text) -> "That means a lot, boss. I've got your back."
            else -> null
        }
    }
}
