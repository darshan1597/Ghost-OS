package com.ghostos.ghost_os.core

import java.util.Locale

data class PhoneContact(val id: String, val name: String, val phone: String)

/** Exact full name wins, otherwise whole name words. Never choose the first ambiguous match. */
object ContactMatcher {
    private fun key(value: String) = value.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
    fun match(query: String, contacts: List<PhoneContact>): List<PhoneContact> {
        val normalized = key(query)
        if (normalized.isEmpty()) return emptyList()
        val unique = contacts.distinctBy { key(it.name) to it.phone }
        val exact = unique.filter { key(it.name) == normalized }
        val words = normalized.split(" ")
        if (exact.isNotEmpty() && words.size > 1) return exact
        return unique.filter { contact -> words.all { it in key(contact.name).split(" ") } }
    }
    fun choose(reply: String, candidates: List<PhoneContact>): PhoneContact? {
        var normalized = key(reply)
        listOf("send it to ", "send to ", "choose ", "select ", "it is ", "number ", "the ").forEach {
            normalized = normalized.removePrefix(it)
        }
        normalized = normalized.removeSuffix(" please").removeSuffix(" one")
        val index = when (normalized) {
            "first", "first one", "one", "1" -> 0
            "second", "second one", "two", "2" -> 1
            "third", "third one", "three", "3" -> 2
            else -> null
        }
        if (index != null) return candidates.getOrNull(index)
        return match(normalized, candidates).singleOrNull()
    }
}
