package com.ghostos.ghost_os.voice

import android.app.KeyguardManager
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import com.ghostos.ghost_os.MainActivity
import com.ghostos.ghost_os.core.GhostRuntime

/** Optional, bounded overlay: touches outside it always reach the underlying app. */
class AssistantBubble(private val context: Context) {
    private val manager = context.getSystemService(WindowManager::class.java)
    private val density = context.resources.displayMetrics.density
    private fun dp(value: Int) = (value * density).toInt()
    private var button: View? = null
    private var panel: View? = null
    private var position: WindowManager.LayoutParams? = null
    private var phase = "idle"
    private var lastResult = 0L
    private var notifyUntil = 0L
    private var label: TextView? = null
    private val main = Handler(Looper.getMainLooper())
    private var fadeScheduled = false
    private val idleFade = Runnable {
        fadeScheduled = false
        if (visible && panel == null && phase == "idle" && GhostRuntime.bubbleFade()) button?.animate()?.alpha(.45f)?.setDuration(300)?.start()
    }
    private fun scheduleFade() {
        if (!fadeScheduled && visible && panel == null && phase == "idle" && GhostRuntime.bubbleFade()) { fadeScheduled = true; main.postDelayed(idleFade, 15000) }
    }
    private fun interact() { main.removeCallbacks(idleFade); fadeScheduled = false; button?.animate()?.cancel(); button?.alpha = 1f; scheduleFade() }
    val visible get() = button?.isAttachedToWindow == true
    private val observer: (Map<String, Any?>) -> Unit = { state ->
        val newest = ((state["history"] as? List<*>)?.firstOrNull() as? Map<*, *>)?.get("time") as? Number
        if (newest != null && newest.toLong() != lastResult) {
            if (lastResult != 0L || visible) notifyUntil = SystemClock.uptimeMillis() + 5000
            lastResult = newest.toLong()
        }
        val status = state["status"].toString().lowercase()
        phase = when {
            (state["link"] as? Map<*, *>)?.get("busy") == true || status.contains("loading") -> "processing"
            status.contains("your command") -> "listening"
            state["speaking"] == true -> "response"
            else -> "idle"
        }
        label?.text = when (phase) { "listening" -> "Listening..."; "processing" -> "Working..."; "response" -> "Responding..."; else -> "Hey Ghost ready" }
        button?.invalidate()
        refresh()
        if (phase != "idle" || SystemClock.uptimeMillis() < notifyUntil || !GhostRuntime.bubbleFade()) interact() else scheduleFade()
    }
    init { GhostRuntime.observe(observer) }
    fun refresh() {
        val unlocked = !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val interactive = context.getSystemService(PowerManager::class.java).isInteractive
        if (GhostRuntime.isForeground() || !GhostRuntime.listening || !GhostRuntime.handsFreeEnabled() || !Settings.canDrawOverlays(context) || !unlocked || !interactive) { hide(); return }
        val requestedSize = dp((GhostRuntime.bubbleSize() * 76f / 50f).toInt())
        if (button != null && position?.width != requestedSize) hide()
        if (button != null) { if (!GhostRuntime.bubbleFade()) interact(); return }
        try {
            val view = OrbitView(context)
            val size = requestedSize
            val params = WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT).apply {
                    gravity = Gravity.TOP or Gravity.LEFT
                    x = (position?.x ?: (context.resources.displayMetrics.widthPixels - size)).coerceIn(0, (context.resources.displayMetrics.widthPixels - size).coerceAtLeast(0))
                    y = (position?.y ?: dp(180)).coerceIn(0, (context.resources.displayMetrics.heightPixels - size).coerceAtLeast(0))
                }
            position = params
            view.contentDescription = "Ghost assistant. Tap for quick controls; drag to move."
            view.setOnClickListener { interact(); togglePanel() }
            view.setOnLongClickListener { interact(); togglePanel(); true }
            var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
            view.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startX = params.x; startY = params.y; moved = false; interact(); false }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX; val dy = event.rawY - downY
                        if (moved || kotlin.math.abs(dx) + kotlin.math.abs(dy) > dp(10)) {
                            moved = true; view.cancelLongPress(); closePanel()
                            params.x = (startX + dx).toInt().coerceIn(0, (context.resources.displayMetrics.widthPixels - size).coerceAtLeast(0))
                            params.y = (startY + dy).toInt().coerceIn(0, (context.resources.displayMetrics.heightPixels - size).coerceAtLeast(0))
                            manager.updateViewLayout(view, params)
                        }; moved
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (moved) { params.x = if (params.x < context.resources.displayMetrics.widthPixels / 2) 0 else context.resources.displayMetrics.widthPixels - size; manager.updateViewLayout(view, params) }; moved
                    }
                    else -> false
                }
            }
            manager.addView(view, params); button = view; interact()
            view.post { GhostRuntime.continueAutomatic() }
        } catch (_: Exception) { hide() }
    }
    private fun open(screen: String = "assistant") {
        val view = button ?: return
        try {
            val animation = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.width, view.height)
            context.startActivity(Intent(context, MainActivity::class.java).putExtra("ghost_tab", screen)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), animation.toBundle())
            hide()
        } catch (_: Exception) { GhostRuntime.setState("Tap the Ghost notification to open the app.") }
    }
    private fun togglePanel() {
        if (panel != null) { closePanel(); return }
        try {
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(12), dp(15), dp(12))
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xF2071833.toInt(), 0xF2030818.toInt())).apply { cornerRadius = dp(22).toFloat(); setStroke(dp(1), 0xFF148BFF.toInt()) }
            }
            fun row(text: String, action: (() -> Unit)? = null): TextView = TextView(context).apply {
                this.text = text; textSize = 15f; setTextColor(0xFFBBD0F4.toInt()); setPadding(dp(8), dp(12), dp(8), dp(12))
                if (action != null) { isClickable = true; setOnClickListener { action() } }
            }.also { layout.addView(it) }
            label = row(when(phase) { "processing" -> "Working..."; "response" -> "Responding..."; "listening" -> "Listening..."; else -> "Hey Ghost ready" }).apply { setTextColor(0xFF36D5FF.toInt()) }
            row("Open Ghost") { open() }
            row("Stop listening") { GhostRuntime.cancel(); GhostRuntime.stopListening?.invoke(); hide() }
            row("Settings") { open("settings") }
            val params = WindowManager.LayoutParams(dp(226), WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
                gravity = Gravity.TOP or Gravity.LEFT
                x = ((position?.x ?: 0) - dp(226)).coerceIn(0, (context.resources.displayMetrics.widthPixels - dp(226)).coerceAtLeast(0))
                y = (position?.y ?: dp(180)).coerceAtMost((context.resources.displayMetrics.heightPixels - dp(245)).coerceAtLeast(0))
            }
            manager.addView(layout, params); panel = layout
        } catch (_: Exception) { closePanel() }
    }
    private fun closePanel() { panel?.let { try { manager.removeView(it) } catch (_: Exception) {} }; panel = null; label = null; scheduleFade() }
    fun hide() { closePanel(); main.removeCallbacks(idleFade); fadeScheduled = false; button?.let { try { manager.removeView(it) } catch (_: Exception) {} }; button = null }
    fun dispose() { GhostRuntime.removeObserver(observer); hide() }
    private inner class OrbitView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.save(); canvas.scale(width / 76f, height / 76f)
            val now = SystemClock.uptimeMillis()
            val pulse = ((kotlin.math.sin(now / 260.0) + 1) / 2).toFloat()
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(38f, 38f, 37f, intArrayOf(0x774739FF, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(38f, 38f, 37f, paint)
            if (phase == "listening") {
                paint.shader = null; paint.style = Paint.Style.STROKE; paint.strokeWidth = .8f; paint.color = 0xFF215BFF.toInt(); paint.alpha = (70 + pulse * 90).toInt()
                canvas.drawCircle(38f, 38f, 29f + pulse * 5, paint); canvas.drawCircle(38f, 38f, 35f + pulse, paint)
            }
            paint.alpha = 255; paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(18f, 15f, 59f, 62f, intArrayOf(0xFF9FFAFF.toInt(), 0xFF1B79FF.toInt(), 0xFFA64CFF.toInt(), 0xFFD9AAFF.toInt()), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(38f, 38f, 25f, paint)
            paint.shader = RadialGradient(38f, 34f, 25f, intArrayOf(0xFF161E55.toInt(), 0xFF614AFF.toInt()), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(38f, 38f, 22.5f, paint)
            paint.shader = null; paint.color = 0xFF03091F.toInt()
            val ghost = Path().apply {
                moveTo(22f, 51f); cubicTo(24f, 44f, 23f, 36f, 24f, 32f); cubicTo(26f, 18f, 50f, 18f, 52f, 32f)
                cubicTo(53f, 37f, 51f, 44f, 54f, 51f); cubicTo(49f, 56f, 46f, 48f, 43f, 52f)
                cubicTo(38f, 59f, 35f, 56f, 32f, 52f); cubicTo(29f, 48f, 26f, 56f, 22f, 51f); close()
            }
            canvas.drawPath(ghost, paint)
            paint.color = Color.WHITE
            canvas.drawOval(RectF(30f, 32f, 36f, 41f), paint); canvas.drawOval(RectF(42f, 32f, 48f, 41f), paint)
            if (phase == "processing") {
                canvas.save(); canvas.rotate(-25f + (now % 6000) * .06f, 38f, 38f)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.2f
                paint.shader = SweepGradient(38f, 38f, intArrayOf(0xFF2BEAFF.toInt(), 0xFF9750FF.toInt(), 0xFF2BEAFF.toInt()), null)
                canvas.drawOval(RectF(7f, 25f, 69f, 51f), paint); canvas.restore()
            }
            if (phase == "response" || now < notifyUntil) {
                paint.shader = null; paint.style = Paint.Style.FILL; paint.color = 0xFF955CFF.toInt(); canvas.drawCircle(57f, 17f, 7f, paint)
                paint.color = Color.WHITE; canvas.drawCircle(57f, 17f, 4f + pulse, paint)
            }
            canvas.restore()
            if (isAttachedToWindow && (phase != "idle" || now < notifyUntil)) postInvalidateDelayed(40)
        }
    }
}
