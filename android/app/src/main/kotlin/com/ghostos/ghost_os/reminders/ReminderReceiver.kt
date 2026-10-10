package com.ghostos.ghost_os.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ghostos.ghost_os.MainActivity
import com.ghostos.ghost_os.core.GhostRuntime

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = ReminderStore(context)
        val id = intent.getLongExtra("id", -1)
        val reminder = store.get(id) ?: return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("ghost_reminders", "Ghost reminders", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(context, id.toInt(), Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(id.toInt(), Notification.Builder(context, "ghost_reminders")
            .setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("Ghost reminder")
            .setContentText(reminder.text).setStyle(Notification.BigTextStyle().bigText(reminder.text))
            .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build())
        store.complete(id)
        GhostRuntime.init(context)
        GhostRuntime.reminderDue(reminder.text)
    }
}

class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { ReminderStore(context).restore() }
}
