package com.ghostos.ghost_os.notifications

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

data class NotificationReplyTarget(val pendingIntent: PendingIntent, val remoteInputs: List<RemoteInput>) {
    fun send(context: Context, message: String) {
        val results = Bundle()
        remoteInputs.filter { it.allowFreeFormInput }.forEach { results.putCharSequence(it.resultKey, message) }
        val fillIn = Intent()
        RemoteInput.addResultsToIntent(remoteInputs.toTypedArray(), fillIn, results)
        if (Build.VERSION.SDK_INT >= 28) RemoteInput.setResultsSource(fillIn, RemoteInput.SOURCE_FREE_FORM_INPUT)
        pendingIntent.send(context, 0, fillIn)
    }
}
data class GhostNotification(
    val key: String, val app: String, val title: String, val text: String,
    val postedAt: Long, val reply: NotificationReplyTarget? = null,
)

object NotificationCenter {
    private val recent = LinkedHashMap<String, GhostNotification>()
    @Volatile var connected = false
        private set
    var changed: (() -> Unit)? = null

    @Synchronized fun posted(value: GhostNotification) {
        recent[value.key] = value
        while (recent.size > 30) recent.remove(recent.keys.first())
        changed?.invoke()
    }
    @Synchronized fun removed(key: String) { recent.remove(key); changed?.invoke() }
    @Synchronized fun latest(limit: Int = 8) = recent.values.sortedByDescending { it.postedAt }.take(limit)
    @Synchronized fun replyMatches(target: String): List<GhostNotification> {
        val query = target.lowercase().trim()
        return recent.values.asSequence().filter { it.reply != null }
            .filter { query.isEmpty() || "${it.app} ${it.title}".lowercase().contains(query) }
            .sortedByDescending { it.postedAt }.toList()
    }
    fun connection(value: Boolean) { connected = value; changed?.invoke() }
    fun accessGranted(context: Context): Boolean {
        val component = ComponentName(context, GhostNotificationListener::class.java)
        return if (Build.VERSION.SDK_INT >= 27) context.getSystemService(android.app.NotificationManager::class.java)
            .isNotificationListenerAccessGranted(component)
        else android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(component.flattenToString()) == true
    }
}

class GhostNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        NotificationCenter.connection(true)
        activeNotifications?.forEach(::onNotificationPosted)
    }
    override fun onListenerDisconnected() { NotificationCenter.connection(false) }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || sbn.isOngoing || sbn.notification.category == Notification.CATEGORY_SERVICE) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim().orEmpty()
        if (title.isEmpty() && text.isEmpty()) return
        val label = runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString() }.getOrDefault(sbn.packageName)
        val reply = sbn.notification.actions?.asSequence()?.mapNotNull { action ->
            val inputs = action.remoteInputs?.filter { it.allowFreeFormInput }.orEmpty()
            if (inputs.isEmpty()) null else NotificationReplyTarget(action.actionIntent, inputs)
        }?.firstOrNull()
        NotificationCenter.posted(GhostNotification(sbn.key, label, title, text, sbn.postTime, reply))
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification) { NotificationCenter.removed(sbn.key) }
}
