package com.ghostos.ghost_os.voice

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class CinematicEffectsTest {
    private val rate = 22050
    @Test fun limiterBoundsLoudSignalsAndRejectsNonfiniteOutput() {
        val source = FloatArray(rate) { if (it % 71 == 0) Float.NaN else (sin(it * .13) * 8).toFloat() }
        val out = CinematicEffects.process(source, rate, 1f)
        assertTrue(out.all { it.isFinite() && abs(it) <= .891252f })
    }
    @Test fun pitchShiftsFrequencyWithoutSlowingDuration() {
        val source = FloatArray(rate * 2) { (.4 * sin(2 * PI * 1000 * it / rate)).toFloat() }
        val out = CinematicEffects.pitch(source, rate, -3.5)
        assertTrue(out.size in source.size..source.size + rate / 20)
        fun power(hz: Int): Double {
            var re = 0.0; var im = 0.0
            for (i in rate / 2 until rate * 3 / 2) {
                val angle = 2 * PI * hz * i / rate
                re += out[i] * cos(angle); im += out[i] * sin(angle)
            }
            return re * re + im * im
        }
        val peak = (750..900).maxBy { power(it) }
        assertEquals(1000 * 2.0.pow(-3.5 / 12), peak.toDouble(), 15.0)
    }
    @Test fun neutralPresetPreservesPcm() {
        val source = FloatArray(4000) { (.2 * sin(it * .1)).toFloat() }
        val out = CinematicEffects.process(source, rate, 0f, CinematicEffects.Preset(pitchSemitones = 0.0))
        assertArrayEquals(source, out, .00001f)
    }
    @Test fun echoAppearsAtConfiguredDelay() {
        val source = FloatArray(4000).apply { this[0] = .3f }
        val preset = CinematicEffects.Preset(pitchSemitones = 0.0, growlMix = 0.0, bassDb = 0.0)
        val out = CinematicEffects.process(source, rate, 1f, preset)
        assertEquals(.027f, out[(rate * .085).roundToInt()], .00001f)
    }
}
