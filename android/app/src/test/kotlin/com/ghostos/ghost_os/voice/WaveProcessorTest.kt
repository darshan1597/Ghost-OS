package com.ghostos.ghost_os.voice

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WaveProcessorTest {
    private fun wave(): ByteArray {
        val b = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()); b.putInt(40); b.put("WAVEfmt ".toByteArray())
        b.putInt(16); b.putShort(1); b.putShort(1); b.putInt(16000); b.putInt(32000)
        b.putShort(2); b.putShort(16); b.put("data".toByteArray()); b.putInt(4)
        b.putShort(12000); b.putShort(-12000)
        return b.array()
    }
    @Test fun validPcmPreservesHeaderAndBypassesAtZero() {
        val input = File.createTempFile("ghost-test", ".wav")
        val output = File.createTempFile("ghost-output", ".wav")
        try {
            input.writeBytes(wave())
            assertTrue(WaveProcessor.process(input, output, 0f))
            assertArrayEquals(input.readBytes(), output.readBytes())
        } finally { input.delete(); output.delete() }
    }
    @Test fun malformedAndUnsupportedFormatsFailWithoutWriting() {
        val input = File.createTempFile("ghost-test", ".wav")
        val output = File.createTempFile("ghost-output", ".wav")
        try {
            input.writeBytes(byteArrayOf(1, 2, 3))
            assertFalse(WaveProcessor.process(input, output, .5f))
            val wrongBits = wave().also { it[34] = 8 }
            input.writeBytes(wrongBits)
            assertFalse(WaveProcessor.process(input, output, .5f))
            val malformed = wave().also { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putInt(40, Int.MAX_VALUE) }
            input.writeBytes(malformed)
            assertFalse(WaveProcessor.process(input, output, .5f))
            assertEquals(0L, output.length())
        } finally { input.delete(); output.delete() }
    }
}
