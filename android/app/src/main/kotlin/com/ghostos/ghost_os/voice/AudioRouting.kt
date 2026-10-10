package com.ghostos.ghost_os.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/** Routes voice capture to an Android communication headset when driving mode requests it. */
object AudioRouting {
    private var routedName: String? = null

    fun refresh(context: Context, preferBluetooth: Boolean): Map<String, Any?> {
        val audio = context.getSystemService(AudioManager::class.java)
        if (!preferBluetooth || Build.VERSION.SDK_INT < 31 ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 31) audio.clearCommunicationDevice()
            routedName = null
            return snapshot(context)
        }
        val device = audio.availableCommunicationDevices.firstOrNull(::isBluetooth)
        routedName = if (device != null && audio.setCommunicationDevice(device)) device.productName?.toString() ?: "Bluetooth device" else null
        return snapshot(context)
    }

    fun release(context: Context) {
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(AudioManager::class.java).clearCommunicationDevice()
        routedName = null
    }

    fun snapshot(context: Context): Map<String, Any?> {
        val allowed = Build.VERSION.SDK_INT < 31 || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        val available = if (Build.VERSION.SDK_INT >= 31 && allowed) {
            context.getSystemService(AudioManager::class.java).availableCommunicationDevices.any(::isBluetooth)
        } else false
        return mapOf("permission" to allowed, "available" to available, "routed" to (routedName != null), "name" to routedName)
    }

    private fun isBluetooth(device: AudioDeviceInfo) = device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        (Build.VERSION.SDK_INT >= 31 && device.type in listOf(AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER))
}
