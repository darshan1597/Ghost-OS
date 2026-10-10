package com.ghostos.ghost_os.emergency

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.telephony.SmsManager
import com.ghostos.ghost_os.MainActivity
import com.ghostos.ghost_os.core.GhostRuntime
import java.text.DateFormat
import java.util.Date

class EmergencyLocationService : Service(), LocationListener {
    companion object {
        const val ACTION_START = "ghost.emergency.START"
        const val ACTION_REMOTE = "ghost.emergency.REMOTE"
        const val ACTION_STOP = "ghost.emergency.STOP"
        private const val CHANNEL = "ghost_emergency"
        private const val NOTIFICATION_ID = 73
        private const val UPDATE_MS = 5 * 60 * 1000L
    }
    private lateinit var locations: LocationManager
    private var firstSent = false
    private var lastSentAt = 0L
    private var remoteOnly = false

    override fun onCreate() {
        super.onCreate()
        locations = getSystemService(LocationManager::class.java)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Emergency location sharing", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSharing(); return START_NOT_STICKY }
        remoteOnly = intent?.action == ACTION_REMOTE
        if (!EmergencyPhraseStore.enabled(this) || !EmergencyPhraseStore.permissionsReady(this)) {
            stopSelf(); return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification())
        EmergencyPhraseStore.setActive(this, true)
        GhostRuntime.emergencyChanged()
        requestLocations()
        newestLastKnown()?.takeIf { System.currentTimeMillis() - it.time <= 120_000L }?.let(::onLocationChanged)
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val stop = PendingIntent.getService(this, 73, Intent(this, javaClass).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(this, 74, Intent(this, MainActivity::class.java).putExtra("ghost_tab", "settings"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Ghost emergency sharing is active")
            .setContentText(if (remoteOnly) "Sending an alert through the linked phone" else "Sending a location update every 5 minutes")
            .setContentIntent(open).setOngoing(true).setVisibility(Notification.VISIBILITY_PRIVATE)
            .addAction(Notification.Action.Builder(null, "Stop sharing", stop).build()).build()
    }

    private fun requestLocations() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try { locations.requestLocationUpdates(provider, 30_000L, 10f, this) } catch (_: Exception) { }
        }
    }

    private fun newestLastKnown(): Location? {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).mapNotNull {
            try { locations.getLastKnownLocation(it) } catch (_: Exception) { null }
        }.maxByOrNull { it.time }
    }

    override fun onLocationChanged(location: Location) {
        val now = System.currentTimeMillis()
        if (firstSent && now - lastSentAt < UPDATE_MS) return
        val label = if (firstSent) "Live location update" else "Emergency alert"
        val text = "${EmergencyPhraseStore.message(this)}\n$label: https://maps.google.com/?q=${location.latitude},${location.longitude}\nAccuracy: ${location.accuracy.toInt()} m · ${DateFormat.getDateTimeInstance().format(Date(now))}"
        if (remoteOnly) {
            try {
                com.ghostos.ghost_os.link.GhostLink.prepareWhatsApp(EmergencyPhraseStore.phone(this), text)
            } catch (_: Exception) { }
            stopSharing()
            return
        }
        try {
            @Suppress("DEPRECATION") val sms = SmsManager.getDefault()
            val parts = sms.divideMessage(text)
            sms.sendMultipartTextMessage(EmergencyPhraseStore.phone(this), null, parts, null, null)
            firstSent = true; lastSentAt = now
            EmergencyPhraseStore.setWhatsAppPending(this, text)
            GhostRuntime.emergencyChanged()
        } catch (_: Exception) { }
    }

    private fun stopSharing() {
        try { locations.removeUpdates(this) } catch (_: Exception) { }
        EmergencyPhraseStore.setActive(this, false)
        GhostRuntime.emergencyChanged()
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
    override fun onDestroy() {
        try { locations.removeUpdates(this) } catch (_: Exception) { }
        EmergencyPhraseStore.setActive(this, false)
        GhostRuntime.emergencyChanged()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
}
