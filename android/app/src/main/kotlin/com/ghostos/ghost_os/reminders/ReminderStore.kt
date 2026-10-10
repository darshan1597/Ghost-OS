package com.ghostos.ghost_os.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class GhostReminder(val id: Long, val text: String, val triggerAt: Long)

class ReminderStore(private val context: Context) : SQLiteOpenHelper(context, "ghost_reminders.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE reminders (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, trigger_at INTEGER NOT NULL, created_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun add(text: String, triggerAt: Long): GhostReminder {
        val values = ContentValues().apply { put("text", text); put("trigger_at", triggerAt); put("created_at", System.currentTimeMillis()) }
        val id = writableDatabase.insertOrThrow("reminders", null, values)
        return GhostReminder(id, text, triggerAt).also(::schedule)
    }
    fun all(): List<GhostReminder> = readableDatabase.query("reminders", arrayOf("id", "text", "trigger_at"), null, null, null, null, "trigger_at ASC").use { cursor ->
        buildList { while (cursor.moveToNext()) add(GhostReminder(cursor.getLong(0), cursor.getString(1), cursor.getLong(2))) }
    }
    fun get(id: Long): GhostReminder? = readableDatabase.query("reminders", arrayOf("id", "text", "trigger_at"), "id=?", arrayOf(id.toString()), null, null, null).use {
        if (it.moveToFirst()) GhostReminder(it.getLong(0), it.getString(1), it.getLong(2)) else null
    }
    fun delete(id: Long) {
        cancel(id); writableDatabase.delete("reminders", "id=?", arrayOf(id.toString()))
    }
    fun complete(id: Long) { writableDatabase.delete("reminders", "id=?", arrayOf(id.toString())) }
    fun restore() { all().forEach { if (it.triggerAt > System.currentTimeMillis()) schedule(it) else complete(it.id) } }

    private fun pending(id: Long) = PendingIntent.getBroadcast(context, id.toInt(), Intent(context, ReminderReceiver::class.java).putExtra("id", id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun schedule(reminder: GhostReminder) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        if (android.os.Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pending(reminder.id))
        else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pending(reminder.id))
    }
    private fun cancel(id: Long) = context.getSystemService(AlarmManager::class.java).cancel(pending(id))
}
