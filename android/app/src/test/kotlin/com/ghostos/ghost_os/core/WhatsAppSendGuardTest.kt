package com.ghostos.ghost_os.core

import org.junit.Assert.*
import org.junit.Test

class WhatsAppSendGuardTest {
    private fun match(titles: List<String>, body: String = "Hi!", unique: Boolean = true) =
        WhatsAppSendGuard.matches("Harsh Kumar", "+919876543210", "Hi!", titles, body, unique)
    @Test fun exactTitleAndUnchangedBodyAreRequired() {
        assertTrue(match(listOf("Harsh Kumar")))
        assertFalse(match(listOf("Harsh Gowda")))
        assertFalse(match(listOf("Harsh")))
        assertFalse(match(listOf("Harsh Kumar"), "old draft"))
        assertFalse(match(listOf("Harsh Kumar"), ""))
    }
    @Test fun duplicateNameCannotAuthoriseANumberByName() {
        assertFalse(match(listOf("Harsh Kumar"), unique = false))
        assertTrue(match(listOf("+91 98765 43210"), unique = false))
        assertFalse(match(listOf("+91 98765 43211"), unique = false))
    }
}
