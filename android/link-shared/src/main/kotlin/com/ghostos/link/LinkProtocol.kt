package com.ghostos.link

import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID

/** Length limits apply before allocating or decoding an untrusted frame. */
object LinkProtocol {
    val SERVICE: UUID = UUID.fromString("f39bfa2c-2f73-4d57-ae93-4e30d8a461f0")
    const val VERSION = 2
    const val LIMIT = 8192
    fun read(input: DataInputStream): String {
        val size = input.readInt()
        require(size in 1..LIMIT) { "Invalid Ghost Link frame size" }
        val bytes = ByteArray(size)
        input.readFully(bytes)
        return bytes.toString(Charsets.UTF_8)
    }
    fun write(output: DataOutputStream, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        require(bytes.size in 1..LIMIT)
        synchronized(output) { output.writeInt(bytes.size); output.write(bytes); output.flush() }
    }
}

/** Replay protection lasts for this connection; reconnect creates a new session. */
class LinkRequestGuard {
    private val seen = mutableSetOf<String>()
    fun accept(id: String, operation: String, name: String = "", phone: String = "", message: String = ""): Boolean {
        val validPayload = when (operation) {
            "open_app" -> name.isNotBlank() && name.length <= 120
            "remote_emergency" -> phone.matches(Regex("\\+?[0-9]{7,15}")) && message.isNotBlank() && message.length <= 1800
            else -> false
        }
        if (!validPayload) return false
        if (runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false).not()) return false
        if (seen.size >= 256) return false // reconnect explicitly; never evict replay IDs
        return seen.add(id)
    }
}

object RemoteCommand {
    private val target = Regex("\\bon (?:my |the )?(?:other|paired|linked|second) (?:phone|device)\\b", RegexOption.IGNORE_CASE)
    fun extract(text: String): String? = if (target.containsMatchIn(text))
        text.replace(target, "").replace(Regex("\\s+"), " ").trim().trimEnd('.', '!', '?').trim() else null
}
