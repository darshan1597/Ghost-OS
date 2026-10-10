package com.ghostos.ghost_os.core

import org.junit.Assert.*
import org.junit.Test

class ContactMatcherTest {
    private val contacts = listOf(PhoneContact("1", "Harsh Kumar", "+919876543210"),
        PhoneContact("2", "Harsh Gowda", "+919876543211"), PhoneContact("3", "Harsha", "+919876543212"))
    @Test fun shortNameKeepsAmbiguityAndExcludesOtherNames() {
        assertEquals(listOf("1", "2"), ContactMatcher.match("Harsh", contacts).map { it.id })
        assertNull(ContactMatcher.choose("Harsh", contacts))
    }
    @Test fun fullNameAndOrdinalChooseOnlyOne() {
        assertEquals("2", ContactMatcher.choose("Harsh Gowda", contacts)?.id)
        assertEquals("2", ContactMatcher.choose("the second one", contacts)?.id)
        assertEquals("1", ContactMatcher.choose("send it to Harsh Kumar please", contacts)?.id)
        assertNull(ContactMatcher.choose("fourth", contacts))
    }
    @Test fun multipleNumbersForSameNameRemainChoices() {
        val sameName = listOf(contacts[0], contacts[0].copy(id = "4", phone = "+919876543219"))
        assertEquals(2, ContactMatcher.match("Harsh Kumar", sameName).size)
        assertNull(ContactMatcher.choose("Harsh Kumar", sameName))
        assertEquals("4", ContactMatcher.choose("number two", sameName)?.id)
    }
    @Test fun shortExactNameStillAsksAndDuplicateRowsCollapse() {
        val list = contacts + PhoneContact("4", "Harsh", "+919876543214") + contacts[0] +
            PhoneContact("5", "Harsh Kumar Work", "+919876543215")
        assertEquals(listOf("1", "2", "4", "5"), ContactMatcher.match("harsh", list).map { it.id })
        assertEquals(1, ContactMatcher.match("Harsh Kumar", list).size)
        assertTrue(ContactMatcher.match("", list).isEmpty())
    }
}
