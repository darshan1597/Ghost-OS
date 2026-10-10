package com.ghostos.ghost_os.core

import org.junit.Assert.*
import org.junit.Test

class WakeCommandExtractorTest {
    @Test fun inlineCommandSurvivesWake() {
        assertEquals("set an alarm for six pm", WakeCommandExtractor.extract("hey ghost set an alarm for six pm", false))
        assertEquals("", WakeCommandExtractor.extract("hey ghost", true))
    }
    @Test fun grammarRescuesFreeDecoderOnlyWithConfirmedWake() {
        assertEquals("open whatsapp", WakeCommandExtractor.extract("hey goes open whatsapp", true))
        assertNull(WakeCommandExtractor.extract("hey goes open whatsapp", false))
    }
    @Test fun unrelatedSpeechNeverBecomesAnInlineCommand() {
        assertNull(WakeCommandExtractor.extract("open whatsapp", false))
        assertEquals("", WakeCommandExtractor.extract("open whatsapp", true))
    }
    @Test fun customWakeNameKeepsInlineCommandAndGhostFallback() {
        assertEquals("open maps", WakeCommandExtractor.extract("hey shadow open maps", false, "Shadow"))
        assertEquals("open maps", WakeCommandExtractor.extract("hey ghost open maps", false, "Shadow"))
    }
}
