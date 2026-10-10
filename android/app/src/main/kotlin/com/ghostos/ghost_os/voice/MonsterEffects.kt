package com.ghostos.ghost_os.voice

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tanh

/** Bounded local PCM processing. Zero strength is a bit-exact bypass. */
object MonsterEffects {
    fun process(samples: ShortArray, sampleRate: Int, channels: Int, strength: Float): ShortArray {
        require(sampleRate in 8000..192000 && channels in 1..2)
        require(samples.size % channels == 0)
        val amount = strength.coerceIn(0f, 1f).toDouble()
        if (amount == 0.0) return samples.copyOf()
        val output = ShortArray(samples.size)
        val bass = DoubleArray(channels)
        val delayFrames = (sampleRate * .085).toInt().coerceAtLeast(1)
        val delay = DoubleArray(delayFrames * channels)
        val coefficient = 1 - kotlin.math.exp(-2 * PI * 190 / sampleRate)
        samples.indices.forEach { i ->
            val channel = i % channels
            val dry = samples[i] / 32768.0
            bass[channel] += coefficient * (dry - bass[channel])
            val ring = sin(2 * PI * 29 * (i / channels).toDouble() / sampleRate)
            val slot = i % delay.size
            val coloured = dry * (1 - .13 * amount) + bass[channel] * .22 * amount +
                dry * ring * .14 * amount + delay[slot] * .12 * amount
            delay[slot] = dry
            val wet = tanh(coloured * 1.15) / 1.15
            output[i] = ((dry * (1 - amount) + wet * amount).coerceIn(-.95, .95) * 32767).toInt().toShort()
        }
        return output
    }
}
