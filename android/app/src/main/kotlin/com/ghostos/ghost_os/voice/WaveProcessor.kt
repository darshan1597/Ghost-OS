package com.ghostos.ghost_os.voice

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Accept only well-formed 16-bit PCM WAV. Other formats retain normal playback. */
object WaveProcessor {
    fun process(input: File, output: File, strength: Float): Boolean {
        if (input.length() !in 44..8_000_000) return false
        val bytes = input.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun tag(offset: Int) = String(bytes, offset, 4, Charsets.US_ASCII)
        if (tag(0) != "RIFF" || tag(8) != "WAVE") return false
        var channels = 0; var rate = 0; var bits = 0; var format = 0
        var dataOffset = -1; var dataSize = 0
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val size = buffer.getInt(offset + 4)
            if (size < 0 || size.toLong() + offset + 8 > bytes.size) return false
            when (tag(offset)) {
                "fmt " -> {
                    if (size < 16) return false
                    format = buffer.getShort(offset + 8).toInt()
                    channels = buffer.getShort(offset + 10).toInt()
                    rate = buffer.getInt(offset + 12)
                    bits = buffer.getShort(offset + 22).toInt()
                }
                "data" -> { dataOffset = offset + 8; dataSize = size }
            }
            offset += 8 + size + size % 2
        }
        if (format != 1 || bits != 16 || channels !in 1..2 || rate !in 8000..192000 ||
            dataOffset < 0 || dataSize % (2 * channels) != 0) return false
        val samples = ShortArray(dataSize / 2) { buffer.getShort(dataOffset + it * 2) }
        val processed = MonsterEffects.process(samples, rate, channels, strength)
        processed.forEachIndexed { i, sample -> buffer.putShort(dataOffset + i * 2, sample) }
        output.writeBytes(bytes)
        return true
    }
}
