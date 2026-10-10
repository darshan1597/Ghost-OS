package com.ghostos.ghost_os.voice

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sin

class MonsterEffectsTest {
    @Test fun zeroStrengthIsExactBypass() {
        val input = shortArrayOf(-32768, -12000, 0, 12000, 32767)
        assertArrayEquals(input, MonsterEffects.process(input, 16000, 1, 0f))
    }
    @Test fun silenceStaysSilent() {
        assertArrayEquals(ShortArray(1000), MonsterEffects.process(ShortArray(1000), 16000, 1, 1f))
    }
    @Test fun effectIsBoundedAndChangesAudio() {
        val input = ShortArray(16000) { (sin(it * .05) * 32767).toInt().toShort() }
        val output = MonsterEffects.process(input, 16000, 1, 1f)
        assertEquals(input.size, output.size)
        assertFalse(input.contentEquals(output))
        assertTrue(output.all { it.toInt() in -31129..31129 })
    }
    @Test fun stereoDoesNotMixChannels() {
        val input = ShortArray(3200) { if (it % 2 == 0) 10000 else 0 }
        val output = MonsterEffects.process(input, 16000, 2, .8f)
        assertTrue(output.indices.filter { it % 2 == 1 }.all { output[it] == 0.toShort() })
    }
}
