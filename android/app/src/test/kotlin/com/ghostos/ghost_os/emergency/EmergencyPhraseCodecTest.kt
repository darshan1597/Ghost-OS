package com.ghostos.ghost_os.emergency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyPhraseCodecTest {
    @Test fun phraseMatchingIsNormalizedAndOneWay() {
        val (salt, hash) = EmergencyPhraseCodec.createSecret("Midnight Shield")
        assertTrue(EmergencyPhraseCodec.matches("  MIDNIGHT   shield ", salt, hash))
        assertFalse(EmergencyPhraseCodec.matches("midnight field", salt, hash))
        assertFalse(hash.contains("Midnight", ignoreCase = true))
    }

    @Test fun trustedPhoneIsNormalized() {
        assertEquals("+919876543210", EmergencyPhraseCodec.normalizePhone("+91 98765 43210"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun publicEmergencyNumberIsRejected() { EmergencyPhraseCodec.normalizePhone("112") }
}
