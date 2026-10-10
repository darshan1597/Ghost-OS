package com.ghostos.ghost_os.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.TelecomManager

object CallActions {
    @Suppress("DEPRECATION")
    fun execute(context: Context, action: String): String {
        check(context.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
            "Allow phone call control in Ghost Settings first."
        }
        val telecom = context.getSystemService(TelecomManager::class.java)
        return when (action) {
            "answer" -> { telecom.acceptRingingCall(); "Answering the call." }
            "reject" -> { check(telecom.endCall()) { "There is no ringing call to reject." }; "Call rejected." }
            "end" -> { check(telecom.endCall()) { "There is no active call to end." }; "Call ended." }
            else -> error("Unsupported call action")
        }
    }
}
