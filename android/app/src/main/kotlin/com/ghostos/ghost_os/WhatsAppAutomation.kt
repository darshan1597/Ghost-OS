package com.ghostos.ghost_os

/** No Accessibility APIs in the standard build. The automation flavor registers its sender. */
object WhatsAppAutomation {
    val available get() = BuildConfig.GHOST_WHATSAPP_AUTOMATION
    interface Sender {
        val busy: Boolean
        fun arm(name: String, phone: String, body: String, unique: Boolean)
        fun cancel()
    }
    var sender: Sender? = null
}
