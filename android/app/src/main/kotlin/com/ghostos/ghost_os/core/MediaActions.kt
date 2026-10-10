package com.ghostos.ghost_os.core

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent

/** Lock-safe controls forwarded to Android's current media session. */
object MediaActions {
    fun execute(context: Context, action: String): String {
        val audio = context.getSystemService(AudioManager::class.java)
        val key = when (action) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> null
        }
        if (key != null) {
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
            return when (action) { "play" -> "Playing media."; "pause" -> "Media paused."; "next" -> "Skipping to the next track."; else -> "Going to the previous track." }
        }
        when (action) {
            "volumeUp" -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
            "volumeDown" -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
            "mute" -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            "unmute" -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            else -> error("Unsupported media action")
        }
        return when (action) { "volumeUp" -> "Media volume increased."; "volumeDown" -> "Media volume decreased."; "mute" -> "Media muted."; else -> "Media unmuted." }
    }
}
