package com.ghostos.link.receiver

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.sin

/** Bounded receiver overlay; it never captures audio or sends commands to the controller. */
internal class LinkBubble(private val service: ReceiverService) {
    private val manager = service.getSystemService(WindowManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val prefs get() = service.getSharedPreferences("link", Context.MODE_PRIVATE)
    private val density = service.resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()
    private val artwork = BitmapFactory.decodeResource(service.resources, R.drawable.link_hero)
    private var view: BadgeView? = null
    private var panel: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var panelStatus: TextView? = null
    private var panelMessage: TextView? = null
    private var wasProcessing = false
    private var pulseUntil = 0L
    private var destroyed = false
    val visible get() = view?.isAttachedToWindow == true
    private val idleFade = Runnable {
        if (visible && panel == null && !service.processing && prefs.getBoolean("bubbleFade", true)) {
            view?.animate()?.alpha(.45f)?.setDuration(300)?.start()
        }
    }
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { refresh() }
    }
    init {
        val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT) }
        if (Build.VERSION.SDK_INT >= 33) service.registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else service.registerReceiver(screenReceiver, filter)
    }
    private fun diameter() = prefs.getInt("bubbleSize", 48).takeIf { it in listOf(40, 48, 56) } ?: 48
    fun refresh() {
        if (destroyed) return
        if (service.processing && !wasProcessing) pulseUntil = SystemClock.uptimeMillis() + 700
        wasProcessing = service.processing
        val unlocked = !service.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val awake = service.getSystemService(PowerManager::class.java).isInteractive
        if (!prefs.getBoolean("bubble", false) || !Settings.canDrawOverlays(service) || !unlocked || !awake || LinkActivity.visible) { hide(); return }
        val size = dp(diameter() + 22)
        if (view != null && params?.width != size) hide()
        if (view == null) {
            val badge = BadgeView(service)
            val metrics = service.resources.displayMetrics
            val initial = WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
                gravity = Gravity.TOP or Gravity.LEFT
                x = if (prefs.getBoolean("bubbleLeft", false)) 0 else (metrics.widthPixels - size).coerceAtLeast(0)
                y = (metrics.heightPixels * prefs.getFloat("bubbleY", .35f)).toInt().coerceIn(dp(32), (metrics.heightPixels - size - dp(64)).coerceAtLeast(dp(32)))
            }
            params = initial
            badge.setOnClickListener { restoreOpacity(); if (panel == null) showPanel() else closePanel(); interact() }
            badge.setOnLongClickListener { restoreOpacity(); showPanel(); interact(); true }
            var downX = 0f; var downY = 0f; var initialX = 0; var initialY = 0; var dragged = false
            badge.setOnTouchListener { _, event ->
                when(event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; initialX = initial.x; initialY = initial.y; dragged = false; interact(); false }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX; val dy = event.rawY - downY
                        if (dragged || abs(dx) + abs(dy) > dp(10)) {
                            dragged = true; badge.cancelLongPress(); closePanel()
                            initial.x = (initialX + dx).toInt().coerceIn(0, (metrics.widthPixels - size).coerceAtLeast(0))
                            initial.y = (initialY + dy).toInt().coerceIn(dp(32), (metrics.heightPixels - size - dp(64)).coerceAtLeast(dp(32)))
                            runCatching { manager.updateViewLayout(badge, initial) }
                        }; dragged
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (dragged) {
                            val left = initial.x + size / 2 < metrics.widthPixels / 2
                            initial.x = if (left) 0 else (metrics.widthPixels - size).coerceAtLeast(0)
                            prefs.edit().putBoolean("bubbleLeft", left).putFloat("bubbleY", initial.y.toFloat() / metrics.heightPixels).apply()
                            runCatching { manager.updateViewLayout(badge, initial) }; interact()
                        }; dragged
                    }
                    else -> false
                }
            }
            try { manager.addView(badge, initial); view = badge; interact() } catch (_: Exception) { hide() }
        }
        if (service.processing) { restoreOpacity(); main.removeCallbacks(idleFade) }
        else if (!prefs.getBoolean("bubbleFade", true)) restoreOpacity()
        else { main.removeCallbacks(idleFade); main.postDelayed(idleFade, 15000) }
        panelStatus?.text = "${controllerName()}\n${if (service.connected) "Connected" else "Not connected"}"
        panelStatus?.setTextColor(if (service.connected) 0xFF00E5AE.toInt() else 0xFFFF675E.toInt())
        panelMessage?.text = service.state
        view?.invalidate(); updateDescription()
    }
    private fun restoreOpacity() { view?.animate()?.cancel(); view?.alpha = 1f }
    private fun interact() { restoreOpacity(); main.removeCallbacks(idleFade); if (prefs.getBoolean("bubbleFade", true) && !service.processing) main.postDelayed(idleFade, 15000); view?.invalidate(); updateDescription() }
    private fun updateDescription() { view?.contentDescription = "Ghost Link, ${if (service.processing) "processing request" else if (service.connected) "connected" else "not connected"}. Tap for quick actions; drag to move." }
    private fun controllerName(): String = runCatching {
        val saved = prefs.getString("controller", null)
        service.getSystemService(BluetoothManager::class.java).adapter?.bondedDevices?.firstOrNull { it.address == saved }?.name
    }.getOrNull() ?: "Approved phone"
    private fun open(intent: Intent) {
        val source = view ?: return
        try {
            service.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), ActivityOptions.makeScaleUpAnimation(source, 0, 0, source.width, source.height).toBundle())
            closePanel()
        } catch (_: Exception) { panelMessage?.text = "Open Ghost Link from its notification to continue." }
    }
    private fun showPanel() {
        if (panel != null || !visible) return
        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(14))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xF5071633.toInt(), 0xF50B0925.toInt())).apply { cornerRadius = dp(23).toFloat(); setStroke(dp(1), 0xFF6444FF.toInt()) }
        }
        fun text(value: String, size: Float, color: Int = 0xFFB8CBF0.toInt()) = TextView(service).apply { text = value; textSize = size; setTextColor(color); setPadding(dp(5), dp(5), dp(5), dp(5)) }
        layout.addView(text("Ghost Link", 19f, Color.WHITE))
        panelStatus = text("${controllerName()}\n${if (service.connected) "Connected" else "Not connected"}", 14f, if (service.connected) 0xFF00E5AE.toInt() else 0xFFFF675E.toInt()).also { layout.addView(it) }
        panelMessage = text(service.state, 12f).also { layout.addView(it) }
        fun tile(title: String, icon: String, red: Boolean = false, action: () -> Unit): View = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(8), dp(8), dp(8), dp(8))
            background = RippleDrawable(ColorStateList.valueOf(0x335BD4FF), GradientDrawable().apply { setColor(0xAA0A1932.toInt()); cornerRadius = dp(14).toFloat(); setStroke(dp(1), if (red) 0xFF653452.toInt() else 0xFF203E72.toInt()) }, null)
            addView(LinkIcon(service, icon, red = red), LinearLayout.LayoutParams(dp(30), dp(30)))
            addView(text(title, 12f, if (red) 0xFFFF647D.toInt() else 0xFFCAD7F2.toInt()).apply { gravity = Gravity.CENTER })
            isClickable = true; isFocusable = true; contentDescription = title; setOnClickListener { interact(); action() }
        }
        fun tiles(left: View, right: View) {
            val row = LinearLayout(service)
            row.addView(left, LinearLayout.LayoutParams(0, -1, 1f)); row.addView(View(service), LinearLayout.LayoutParams(dp(8), 1)); row.addView(right, LinearLayout.LayoutParams(0, -1, 1f))
            layout.addView(row, LinearLayout.LayoutParams(-1, dp(88)).apply { topMargin = dp(8) })
        }
        tiles(tile("Open Ghost Link", "phone") { open(Intent(service, LinkActivity::class.java)) }, tile("Check connection", "refresh") {
            val bluetooth = runCatching { service.getSystemService(BluetoothManager::class.java).adapter?.isEnabled == true }.getOrDefault(false)
            panelMessage?.text = if (!bluetooth) "Bluetooth is off." else if (service.connected) "Connected to your approved phone." else "No active connection. Waiting for your approved phone."
        })
        tiles(tile("Bluetooth settings", "bluetooth") { open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }, tile("Disconnect", "disconnect", red = true) { service.stopSelf() })
        layout.setOnTouchListener { _, event -> if (event.action == MotionEvent.ACTION_OUTSIDE) { closePanel(); interact(); true } else false }
        val metrics = service.resources.displayMetrics
        val width = dp(270).coerceAtMost(metrics.widthPixels - dp(20))
        val placement = WindowManager.LayoutParams(width, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = if ((params?.x ?: 0) < metrics.widthPixels / 2) dp(8) else (metrics.widthPixels - width - dp(8)).coerceAtLeast(0)
            y = ((params?.y ?: dp(140)) + (params?.height ?: dp(70))).coerceIn(dp(32), (metrics.heightPixels - dp(380)).coerceAtLeast(dp(32)))
        }
        try { manager.addView(layout, placement); panel = layout } catch (_: Exception) { closePanel() }
    }
    private fun closePanel() { panel?.let { runCatching { manager.removeView(it) } }; panel = null; panelStatus = null; panelMessage = null }
    private fun hide() { main.removeCallbacks(idleFade); closePanel(); view?.let { runCatching { manager.removeView(it) } }; view = null; restoreOpacity() }
    fun dispose() { destroyed = true; main.removeCallbacksAndMessages(null); runCatching { service.unregisterReceiver(screenReceiver) }; hide() }
    private inner class BadgeView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val scale = width / 80f
            canvas.save(); canvas.scale(scale, scale)
            paint.style = Paint.Style.FILL
                val pulse = ((sin(SystemClock.uptimeMillis() / 260.0) + 1) / 2).toFloat()
                paint.shader = RadialGradient(40f, 40f, 39f, intArrayOf(0x663932FF, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
                canvas.drawCircle(40f, 40f, 39f, paint)
                if (service.processing || SystemClock.uptimeMillis() < pulseUntil) {
                    paint.shader = null; paint.style = Paint.Style.STROKE; paint.strokeWidth = .8f; paint.color = 0xFF1357FF.toInt(); paint.alpha = (50 + pulse * 100).toInt()
                    canvas.drawCircle(40f, 40f, 34f + pulse * 3f, paint); canvas.drawCircle(40f, 40f, 39f, paint)
                }
                paint.alpha = 255; paint.style = Paint.Style.FILL
                paint.shader = LinearGradient(12f, 12f, 68f, 68f, intArrayOf(0xFF13DEFF.toInt(), 0xFF296CFF.toInt(), 0xFFB33EFF.toInt()), null, Shader.TileMode.CLAMP)
                canvas.drawCircle(40f, 40f, 29.5f, paint)
                paint.shader = RadialGradient(35f, 25f, 44f, intArrayOf(0xFF0B2261.toInt(), 0xFF04071C.toInt()), null, Shader.TileMode.CLAMP)
                canvas.drawCircle(40f, 40f, 27.7f, paint)
                paint.shader = null; paint.alpha = if (service.connected || service.processing) 255 else 150
                canvas.drawBitmap(artwork, null, RectF(15f, 15f, 65f, 65f), paint)
                paint.alpha = 255; paint.color = if (service.connected) 0xFF00E5AE.toInt() else 0xFFFF453C.toInt()
                canvas.drawCircle(62f, 61f, 6.5f, paint)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f; paint.color = 0xFF060B24.toInt(); canvas.drawCircle(62f, 61f, 7.5f, paint)
            canvas.restore()
            if (isAttachedToWindow && (service.processing || SystemClock.uptimeMillis() < pulseUntil)) postInvalidateDelayed(40)
        }
    }
}
