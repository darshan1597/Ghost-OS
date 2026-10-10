package com.ghostos.ghost_os.core

/** Only a matching full chat title and an unchanged, nonempty composer can authorise one tap. */
object WhatsAppSendGuard {
    fun matches(expectedName: String, expectedPhone: String, expectedBody: String,
        titles: List<String>, body: String, nameIsUnique: Boolean): Boolean {
        if (expectedBody.isBlank() || body != expectedBody) return false
        val phone = expectedPhone.filter { it.isDigit() }
        return titles.any { title ->
            (nameIsUnique && title.trim().equals(expectedName.trim(), ignoreCase = true)) ||
                (title.filter { it.isDigit() } == phone && phone.length >= 7)
        }
    }
}
