package com.ghostos.ghost_os.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import java.util.Locale

object PhoneContacts {
    fun allowed(context: Context) = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    fun read(context: Context): List<PhoneContact> {
        check(allowed(context)) { "Allow contacts in Settings > WhatsApp contacts first." }
        val country = try { context.getSystemService(TelephonyManager::class.java).networkCountryIso }
            catch (_: Exception) { "" }
        val region = (country?.takeIf { it.isNotBlank() } ?: Locale.getDefault().country).uppercase(Locale.ROOT)
        val columns = arrayOf(ContactsContract.CommonDataKinds.Phone._ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
        val contacts = mutableListOf<PhoneContact>()
        context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, columns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val raw = cursor.getString(3)?.takeIf { it.startsWith("+") } ?: cursor.getString(2) ?: continue
                val phone = if (raw.startsWith("+")) "+" + raw.drop(1).filter { it.isDigit() }
                    else PhoneNumberUtils.formatNumberToE164(raw, region) ?: continue
                if (!phone.matches(Regex("\\+[1-9]\\d{6,14}"))) continue
                val name = cursor.getString(1)?.takeIf { it.isNotBlank() } ?: continue
                contacts.add(PhoneContact(cursor.getString(0), name, phone))
            }
        }
        return contacts.distinctBy { it.name to it.phone }.sortedBy { it.name.lowercase(Locale.ROOT) }
    }
}
