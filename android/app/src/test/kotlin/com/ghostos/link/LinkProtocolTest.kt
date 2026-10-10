package com.ghostos.link

import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.util.UUID

class LinkProtocolTest {
    @Test fun framesHandleUnicodeAndBackToBackPackets() {
        val bytes = ByteArrayOutputStream()
        val out = DataOutputStream(bytes)
        LinkProtocol.write(out, "Ghost 👻")
        LinkProtocol.write(out, "second")
        val input = DataInputStream(ByteArrayInputStream(bytes.toByteArray()))
        assertEquals("Ghost 👻", LinkProtocol.read(input)); assertEquals("second", LinkProtocol.read(input))
    }
    @Test fun oversizedFrameRejectedBeforeBodyRead() {
        val bytes = ByteArrayOutputStream(); DataOutputStream(bytes).writeInt(8193)
        assertThrows(IllegalArgumentException::class.java) { LinkProtocol.read(DataInputStream(ByteArrayInputStream(bytes.toByteArray()))) }
    }
    @Test fun truncatedFrameCannotBecomeACommand() {
        val bytes = ByteArrayOutputStream(); val output = DataOutputStream(bytes)
        output.writeInt(20); output.write(byteArrayOf(1, 2))
        assertThrows(EOFException::class.java) { LinkProtocol.read(DataInputStream(ByteArrayInputStream(bytes.toByteArray()))) }
    }
    @Test fun rejectsReplayUnsupportedOperationsAndBadIds() {
        val guard = LinkRequestGuard(); val id = UUID.randomUUID().toString()
        assertFalse(guard.accept(id, "send_message", "Harsh"))
        assertTrue(guard.accept(id, "open_app", "WhatsApp"))
        assertFalse(guard.accept(id, "open_app", "WhatsApp"))
        assertFalse(guard.accept("invalid", "open_app", "WhatsApp"))
        assertFalse(guard.accept(UUID.randomUUID().toString(), "open_app", ""))
    }
    @Test fun validatesRemoteWhatsAppDraftWithoutAllowingArbitraryOperations() {
        val guard = LinkRequestGuard(); val id = UUID.randomUUID().toString()
        assertTrue(guard.accept(id, "remote_emergency", phone = "+919876543210", message = "Help: https://maps.google.com/?q=1,2"))
        assertFalse(guard.accept(UUID.randomUUID().toString(), "remote_emergency", phone = "112", message = "Help"))
        assertFalse(guard.accept(UUID.randomUUID().toString(), "remote_emergency", phone = "+919876543210", message = ""))
    }
    @Test fun routingRequiresExplicitRemoteTarget() {
        assertEquals("open WhatsApp", RemoteCommand.extract("open WhatsApp on my other phone"))
        assertEquals("open chrome", RemoteCommand.extract("open chrome on the paired device."))
        assertEquals("set an alarm for 6pm", RemoteCommand.extract("set an alarm on my other phone for 6pm"))
        assertEquals("open Chrome", RemoteCommand.extract("on my second phone open Chrome"))
        assertNull(RemoteCommand.extract("open WhatsApp"))
    }
}
