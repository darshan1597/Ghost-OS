package com.ghostos.ghost_os.emergency

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class EmergencyPhraseMatch { NONE, START, REMOTE, STOP }

object EmergencyPhraseCodec {
    private val blockedNumbers = setOf("000", "100", "101", "102", "108", "110", "112", "119", "911", "999")

    fun normalizePhrase(value: String) = value.lowercase().trim().replace(Regex("\\s+"), " ")

    fun normalizePhone(value: String): String {
        val trimmed = value.trim()
        val digits = trimmed.filter(Char::isDigit)
        require(digits.length in 7..15) { "Enter a valid trusted contact number." }
        require(digits !in blockedNumbers) { "Use a trusted personal contact, not a public emergency number." }
        return if (trimmed.startsWith("+")) "+$digits" else digits
    }

    fun createSecret(value: String): Pair<String, String> {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        return encode(salt) to encode(derive(normalizePhrase(value), salt))
    }

    fun matches(value: String, salt: String, hash: String): Boolean = try {
        MessageDigest.isEqual(derive(normalizePhrase(value), decode(salt)), decode(hash))
    } catch (_: Exception) { false }

    private fun derive(value: String, salt: ByteArray): ByteArray = SecretKeyFactory
        .getInstance("PBKDF2WithHmacSHA256")
        .generateSecret(PBEKeySpec(value.toCharArray(), salt, 120_000, 256)).encoded
    private fun encode(value: ByteArray) = Base64.getEncoder().encodeToString(value)
    private fun decode(value: String) = Base64.getDecoder().decode(value)
}

object EmergencyPhraseStore {
    private const val PREFS = "ghost_emergency"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun configure(context: Context, phrase: String, remotePhrase: String, stopPhrase: String, phone: String, message: String) {
        val start = EmergencyPhraseCodec.normalizePhrase(phrase)
        val remote = EmergencyPhraseCodec.normalizePhrase(remotePhrase)
        val stop = EmergencyPhraseCodec.normalizePhrase(stopPhrase)
        require(start.length >= 3) { "The emergency phrase must be at least 3 characters." }
        require(remote.length >= 3) { "The Ghost Link emergency phrase must be at least 3 characters." }
        require(stop.length >= 3) { "The stop phrase must be at least 3 characters." }
        require(setOf(start, remote, stop).size == 3) { "All emergency phrases must be different." }
        val normalizedPhone = EmergencyPhraseCodec.normalizePhone(phone)
        val (startSalt, startHash) = EmergencyPhraseCodec.createSecret(start)
        val (remoteSalt, remoteHash) = EmergencyPhraseCodec.createSecret(remote)
        val (stopSalt, stopHash) = EmergencyPhraseCodec.createSecret(stop)
        prefs(context).edit()
            .putString("startSalt", startSalt).putString("startHash", startHash)
            .putString("remoteSalt", remoteSalt).putString("remoteHash", remoteHash)
            .putString("stopSalt", stopSalt).putString("stopHash", stopHash)
            .putString("phone", normalizedPhone)
            .putString("message", message.trim().ifEmpty { "I may need help. Please check my location." }.take(160))
            .putBoolean("configured", true).putBoolean("enabled", false).apply()
    }

    fun match(context: Context, value: String): EmergencyPhraseMatch {
        val p = prefs(context)
        if (!p.getBoolean("configured", false)) return EmergencyPhraseMatch.NONE
        val start = EmergencyPhraseCodec.matches(value, p.getString("startSalt", "")!!, p.getString("startHash", "")!!)
        if (start) return EmergencyPhraseMatch.START
        val remote = EmergencyPhraseCodec.matches(value, p.getString("remoteSalt", "")!!, p.getString("remoteHash", "")!!)
        if (remote) return EmergencyPhraseMatch.REMOTE
        val stop = EmergencyPhraseCodec.matches(value, p.getString("stopSalt", "")!!, p.getString("stopHash", "")!!)
        return if (stop) EmergencyPhraseMatch.STOP else EmergencyPhraseMatch.NONE
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        require(!enabled || prefs(context).getBoolean("configured", false)) { "Set up the trusted contact and phrases first." }
        require(!enabled || permissionsReady(context)) { "Allow SMS and location, including location access all the time, first." }
        prefs(context).edit().putBoolean("enabled", enabled).apply()
    }

    fun enabled(context: Context) = prefs(context).getBoolean("enabled", false)
    fun phone(context: Context) = prefs(context).getString("phone", "") ?: ""
    fun message(context: Context) = prefs(context).getString("message", "") ?: ""
    fun active(context: Context) = prefs(context).getBoolean("active", false)
    fun setActive(context: Context, value: Boolean) = prefs(context).edit().putBoolean("active", value).apply()
    fun setWhatsAppPending(context: Context, value: String?) = prefs(context).edit().apply {
        if (value == null) remove("whatsappPending") else putString("whatsappPending", value)
    }.apply()
    fun takeWhatsAppPending(context: Context): String? {
        val p = prefs(context); val value = p.getString("whatsappPending", null)
        if (value != null) p.edit().remove("whatsappPending").apply()
        return value
    }
    fun clear(context: Context) { prefs(context).edit().clear().apply() }

    fun permissionsReady(context: Context): Boolean {
        val sms = context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val background = Build.VERSION.SDK_INT < 29 || context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        return sms && fine && background
    }

    fun snapshot(context: Context): Map<String, Any?> {
        val p = prefs(context); val phone = p.getString("phone", "") ?: ""
        return mapOf(
            "configured" to p.getBoolean("configured", false),
            "enabled" to enabled(context), "active" to active(context),
            "permissionsReady" to permissionsReady(context),
            "phoneMasked" to if (phone.length >= 4) "••••${phone.takeLast(4)}" else ""
        )
    }
}
