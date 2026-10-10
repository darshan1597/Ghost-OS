package com.ghostos.ghost_os

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import com.ghostos.ghost_os.core.GhostRuntime

/** Only a generic prompt is visible over the lock screen, never Flutter/private data. */
class UnlockActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GhostRuntime.init(this)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContentView(TextView(this).apply {
            text = "Unlock your phone using Android authentication to continue with Ghost."
            gravity = Gravity.CENTER; textSize = 20f; setPadding(32, 32, 32, 32)
        })
        window.decorView.post {
            getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        GhostRuntime.unlockResult("Phone unlocked through Android authentication.")
                        startActivity(Intent(this@UnlockActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                        finish()
                    }
                    override fun onDismissCancelled() { GhostRuntime.unlockResult("Unlock cancelled. Pending actions remain paused."); finish() }
                    override fun onDismissError() { GhostRuntime.unlockResult("Android could not show the unlock prompt. Unlock your phone normally."); finish() }
                })
        }
    }
}
