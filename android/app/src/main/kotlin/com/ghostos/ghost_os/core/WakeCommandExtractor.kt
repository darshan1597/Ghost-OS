package com.ghostos.ghost_os.core

object WakeCommandExtractor {
    fun extract(text: String, grammarDetected: Boolean, assistantName: String = "Ghost"): String? {
        val name = Regex.escape(assistantName.trim().lowercase()).replace("\\ ", "\\s+")
        val phrase = Regex("\\bhey\\s+(?:$name|ghosts?)\\b", RegexOption.IGNORE_CASE).find(text)
        if (phrase != null) return text.substring(phrase.range.last + 1).trim()
        if (!grammarDetected) return null
        // A confirmed grammar hit may decode as 'hey goes' in the free recognizer.
        // Do not strip arbitrary words from unrelated speech to create an action.
        return if (text.startsWith("hey ")) text.split(" ").drop(2).joinToString(" ").trim() else ""
    }
}
