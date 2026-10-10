package com.ghostos.ghost_os.link

import android.app.*
import android.content.Intent
import android.os.IBinder
import com.ghostos.ghost_os.MainActivity

class ControllerLinkService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "stop") { stopSelf(); return START_NOT_STICKY }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("ghost_link", "Ghost Link", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, ControllerLinkService::class.java).setAction("stop"), PendingIntent.FLAG_IMMUTABLE)
        startForeground(31, Notification.Builder(this, "ghost_link").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Ghost Link connection").setContentText("Tap to manage; disconnect anytime.").setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Disconnect", stop).build()).setOngoing(true).build())
        try { GhostLink.connect(intent?.getStringExtra("address") ?: "") }
        catch (_: Exception) { GhostLink.disconnect("Unable to connect. Check Bluetooth permission and pairing."); stopSelf() }
        return START_NOT_STICKY
    }
    override fun onDestroy() { GhostLink.disconnect(); super.onDestroy() }
}
