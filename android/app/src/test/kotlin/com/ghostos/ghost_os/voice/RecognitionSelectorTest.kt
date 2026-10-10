package com.ghostos.ghost_os.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionSelectorTest {
    @Test fun validCommandBeatsHigherConfidenceNonsense() {
        val values = listOf(RecognitionCandidate("set and long for six pm", .92), RecognitionCandidate("set an alarm for six pm", .74))
        assertEquals("set an alarm for six pm", RecognitionSelector.select(values, true))
    }
    @Test fun confidenceWinsWhenCandidatesHaveSameMeaningfulness() {
        val values = listOf(RecognitionCandidate("open mail", .63), RecognitionCandidate("open gmail", .89))
        assertEquals("open gmail", RecognitionSelector.select(values, true))
    }
    @Test fun ordinarySingleResultRemainsCompatible() {
        assertEquals("hey ghost", RecognitionSelector.select(listOf(RecognitionCandidate("hey ghost", .8)), false))
    }
    @Test fun lowConfidenceUnknownSpeechRequestsRecovery() {
        val decision = RecognitionSelector.decide(listOf(
            RecognitionCandidate("send harsh a massage", .42),
            RecognitionCandidate("send harsh a message", .37),
        ), true)
        assertTrue(decision.uncertain)
        assertEquals("send harsh a message", decision.alternative)
    }
    @Test fun recognizedActionDoesNotGetBlockedByLowAcousticConfidence() {
        val decision = RecognitionSelector.decide(listOf(RecognitionCandidate("open whatsapp", .45)), true)
        assertTrue(decision.understoodAction)
        assertFalse(decision.uncertain)
    }
}
