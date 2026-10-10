package com.ghostos.ghost_os.voice

import kotlin.math.*

/** Local mono PCM DSP. Pitch uses overlapping delay grains, never playback-rate changes.
 * Formant shifting is intentionally not enabled. Parameters are independent of the TTS engine.
 */
object CinematicEffects {
    data class Preset(
        val pitchSemitones: Double = -3.5,
        val speed: Float = .92f,
        val growlMix: Double = .12,
        val echoMs: Double = 85.0,
        val echoMix: Double = .09,
        val bassDb: Double = 3.0,
        val ceilingDb: Double = -1.0,
    )
    val ghost = Preset()

    fun process(input: FloatArray, rate: Int, strength: Float, preset: Preset = ghost): FloatArray {
        require(rate in 8000..192000)
        require(strength.isFinite())
        val amount = strength.coerceIn(0f, 1f).toDouble()
        // Strength changes coloration; pitch and speaking speed remain properties of the preset.
        val pitched = pitch(input, rate, preset.pitchSemitones)
        val delay = (rate * preset.echoMs / 1000).roundToInt().coerceAtLeast(1)
        val output = FloatArray(pitched.size + if (amount > 0) delay else 0)
        val shelf = Shelf(rate, preset.bassDb * amount)
        val limit = 10.0.pow(preset.ceilingDb / 20)
        var peak = 0.0
        for (i in output.indices) {
            val dry = if (i < pitched.size) pitched[i].toDouble() else 0.0
            val grown = dry * (1 - preset.growlMix * amount) +
                tanh(dry * 2.2) / 2.2 * preset.growlMix * amount
            val echo = if (i >= delay && i - delay < pitched.size) pitched[i - delay] * preset.echoMix * amount else 0.0
            val value = shelf.next(grown + echo).let { if (it.isFinite()) it else 0.0 }
            output[i] = value.toFloat(); peak = max(peak, abs(value))
        }
        // Whole-chunk peak limiter: no clipping, no gain pumping within a sentence.
        val gain = if (peak > limit) limit / peak else 1.0
        for (i in output.indices) output[i] = (output[i] * gain).toFloat()
        return output
    }

    /** Two smoothly cross-faded variable-delay read heads; duration stays unchanged.
     * A short padded tail retains the delayed ending. This lightweight effect can color
     * consonants; it is not a studio formant-preserving pitch shifter.
     */
    internal fun pitch(input: FloatArray, rate: Int, semitones: Double): FloatArray {
        require(semitones in -6.0..0.0)
        if (semitones == 0.0 || input.isEmpty()) return input.copyOf()
        val ratio = 2.0.pow(semitones / 12)
        val span = (rate * .045).toInt().coerceAtLeast(2)
        fun read(at: Double): Double {
            val index = floor(at).toInt(); val fraction = at - index
            val a = input.getOrNull(index)?.toDouble() ?: 0.0
            val b = input.getOrNull(index + 1)?.toDouble() ?: 0.0
            return a + (b - a) * fraction
        }
        return FloatArray(input.size + span) { i ->
            val phase = (i * (1 - ratio) / span) % 1.0
            val other = (phase + .5) % 1.0
            val weight = .5 - .5 * cos(2 * PI * phase)
            (read(i - phase * span) * weight + read(i - other * span) * (1 - weight)).toFloat()
        }
    }

    /** RBJ low-shelf biquad at 180 Hz, slope 1. */
    private class Shelf(rate: Int, db: Double) {
        private val a = 10.0.pow(db / 40)
        private val w = 2 * PI * 180 / rate
        private val c = cos(w)
        private val alpha = sin(w) / 2 * sqrt(2.0)
        private val beta = 2 * sqrt(a) * alpha
        private val a0 = (a + 1) + (a - 1) * c + beta
        private val b0 = a * ((a + 1) - (a - 1) * c + beta) / a0
        private val b1 = 2 * a * ((a - 1) - (a + 1) * c) / a0
        private val b2 = a * ((a + 1) - (a - 1) * c - beta) / a0
        private val a1 = -2 * ((a - 1) + (a + 1) * c) / a0
        private val a2 = ((a + 1) + (a - 1) * c - beta) / a0
        private var z1 = 0.0; private var z2 = 0.0
        fun next(x: Double): Double {
            val y = b0 * x + z1
            z1 = b1 * x - a1 * y + z2; z2 = b2 * x - a2 * y
            return y
        }
    }
}
