package com.ghostos.ghost_os

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ghostos.ghost_os.core.GhostRuntime
import com.ghostos.ghost_os.core.WhatsAppSendGuard

/** User enabled, WhatsApp only. A fresh explicit message command arms exactly one Send tap. */
class WhatsAppAccessService : AccessibilityService(), WhatsAppAutomation.Sender {
    private data class Task(val name: String, val phone: String, val body: String,
        val unique: Boolean, val started: Long = SystemClock.elapsedRealtime())
    private val main = Handler(Looper.getMainLooper())
    private var task: Task? = null
    override val busy get() = task != null
    private var checking = false
    override fun onServiceConnected() { GhostRuntime.init(this); WhatsAppAutomation.sender = this; GhostRuntime.environmentChanged() }
    override fun arm(name: String, phone: String, body: String, unique: Boolean) {
        cancel()
        val next = Task(name, phone, body, unique)
        task = next
        main.postDelayed({ if (task === next) finish("I left the WhatsApp draft open. I couldn't verify the chat and Send button; please send it yourself.") }, 30000)
    }
    override fun cancel() { task = null; checking = false; main.removeCallbacksAndMessages(null) }
    private fun finish(message: String) { cancel(); GhostRuntime.messageAutomationResult(message) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val next = task ?: return
        if (SystemClock.elapsedRealtime() - next.started > 30000) { cancel(); return }
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked) { finish("Your phone locked. I cancelled automatic Send."); return }
        if (event?.packageName?.toString() != "com.whatsapp" || checking) return
        if (!verified(next)) return
        checking = true
        // Recheck after navigation settles, then clear the task BEFORE performing one tap.
        main.postDelayed({
            checking = false
            if (task !== next || getSystemService(KeyguardManager::class.java).isKeyguardLocked || !verified(next)) return@postDelayed
            val root = rootInActiveWindow ?: return@postDelayed
            val send = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
                .singleOrNull { it.isVisibleToUser && it.isEnabled && it.isClickable } ?: return@postDelayed
            cancel()
            val clicked = send.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            GhostRuntime.messageAutomationResult(if (clicked) "I tapped Send for ${next.name}, boss. Check WhatsApp for delivery."
                else "The Send button didn't respond. Your draft is ready in WhatsApp.")
        }, 400)
    }
    private fun verified(next: Task): Boolean {
        val root = rootInActiveWindow ?: return false
        if (root.packageName?.toString() != "com.whatsapp") return false
        val titles = listOf("conversation_contact_name")
            .flatMap { root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/$it") }
            .filter { it.isVisibleToUser }.mapNotNull { it.text?.toString() }
        val composer = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
            .singleOrNull { it.isVisibleToUser && it.isEditable }?.text?.toString() ?: return false
        return WhatsAppSendGuard.matches(next.name, next.phone, next.body, titles, composer, next.unique)
    }
    override fun onInterrupt() { if (task != null) finish("WhatsApp assistance was interrupted. Please check the draft.") }
    override fun onDestroy() { cancel(); WhatsAppAutomation.sender = null; GhostRuntime.environmentChanged(); super.onDestroy() }
}
