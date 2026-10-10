package com.ghostos.link.receiver

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** Local receiver controls. Visual state always comes from the actual service. */
class LinkActivity : Activity() {
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var detail: TextView
    private lateinit var connectionIcon: LinkIcon
    private lateinit var devices: Spinner
    private lateinit var receiving: Switch
    private lateinit var bubble: Switch
    private var addresses = emptyList<String>()
    private var updating = false
    private val prefs get() = getSharedPreferences("link", MODE_PRIVATE)
    private val white = Color.WHITE
    private val muted = 0xFFADC2E9.toInt()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    companion object { var visible = false }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF030A1A.toInt()
        window.navigationBarColor = 0xFF030A1A.toInt()
        val root = android.widget.FrameLayout(this).apply { background = LinkBackdrop(); fitsSystemWindows = true }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24)) }
        root.addView(ScrollView(this).apply { isFillViewport = true; addView(content); clipToPadding = false })
        setContentView(root)
        content.addView(LinkHeader(this), LinearLayout.LayoutParams(-1, dp(145)))

        val features = card()
        val strip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        features.addView(strip)
        listOf("apps" to "Receives apps and emergency SMS", "mic" to "No microphone\nNo AI model", "bluetooth" to "Works offline via Bluetooth").forEachIndexed { index, (icon, text) ->
            val column = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            strip.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
            column.addView(LinkIcon(this, icon), LinearLayout.LayoutParams(dp(30), dp(30)))
            column.addView(label(text, 10.5f).apply { setPadding(dp(5), 0, dp(3), 0) }, LinearLayout.LayoutParams(0, -2, 1f))
            if (index < 2) strip.addView(View(this).apply { setBackgroundColor(0xFF123050.toInt()) }, LinearLayout.LayoutParams(dp(1), dp(52)))
        }

        val connection = card()
        val connectionRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        connectionIcon = LinkIcon(this, "bluetooth")
        connectionRow.addView(connectionIcon, LinearLayout.LayoutParams(dp(70), dp(70)))
        val connectionText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0) }
        connectionRow.addView(connectionText, LinearLayout.LayoutParams(0, -2, 1f))
        status = label("Receiving is off", 20f, true); detail = label("Choose and approve your main phone below.", 13f)
        connectionText.addView(status); connectionText.addView(detail)
        connection.addView(connectionRow)
        action(connection, "Pair phones in Bluetooth settings", "bluetooth", true) { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        action(connection, "Allow nearby devices and refresh", "refresh") { permissions() }

        val controller = card()
        header(controller, "Approved controller", "Only the selected phone can send requests.", "phone")
        devices = Spinner(this).apply { setPadding(dp(10), dp(6), dp(6), dp(6)); backgroundTintList = ColorStateList.valueOf(0xFF52AFFF.toInt()) }
        val picker = FrameLayout(this).apply { background = background(); addView(devices, FrameLayout.LayoutParams(-1, -1)) }
        controller.addView(picker, LinearLayout.LayoutParams(-1, dp(55)).apply { topMargin = dp(8) })
        receiving = toggle(controller, "Enable receiving", "Receive app requests and emergency SMS from the approved phone.", "bluetooth") { enabled ->
            if (enabled) beginReceiving() else { stopService(Intent(this, ReceiverService::class.java)); updateState("Receiving is off") }
        }

        val bubbleCard = card()
        bubble = toggle(bubbleCard, "Connection bubble", "Show a small floating bubble while receiving (required outside this screen).", "bubble") { enabled ->
            prefs.edit().putBoolean("bubble", enabled).apply()
            if (enabled && !Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            else ReceiverService.instance?.refreshBubble()
            syncControls()
        }
        val sizes = Spinner(this).apply {
            contentDescription = "Connection bubble size"
            adapter = object : ArrayAdapter<String>(this@LinkActivity, android.R.layout.simple_spinner_item, listOf("Small (40 dp)", "Medium (48 dp)", "Large (56 dp)")) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = super.getView(position, convertView, parent).also { (it as TextView).apply { setTextColor(white); textSize = 13f } }
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = super.getDropDownView(position, convertView, parent).also { (it as TextView).apply { setTextColor(white); setBackgroundColor(0xFF091A31.toInt()); setPadding(dp(14), dp(12), dp(14), dp(12)) } }
            }.apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(listOf(40, 48, 56).indexOf(prefs.getInt("bubbleSize", 48)).coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selected = listOf(40, 48, 56)[position]
                    if (prefs.getInt("bubbleSize", 48) != selected) { prefs.edit().putInt("bubbleSize", selected).apply(); ReceiverService.instance?.refreshBubble() }
                }
            }
        }
        bubbleCard.addView(label("Bubble size", 12f).apply { setPadding(dp(3), dp(10), 0, 0) })
        bubbleCard.addView(sizes, LinearLayout.LayoutParams(-1, dp(44)))
        val minimizeControl = toggle(bubbleCard, "Fade when idle", "Keep the same size at 45% opacity after 15 seconds. Tap to brighten.", "bubble") { enabled ->
            prefs.edit().putBoolean("bubbleFade", enabled).apply(); ReceiverService.instance?.refreshBubble()
        }
        updating = true; minimizeControl.isChecked = prefs.getBoolean("bubbleFade", true); updating = false
        bubbleCard.addView(label("Drag the bubble to either edge. Green means connected; red means waiting. Pulsing rings show command handling. Tap for quick actions.", 12f).apply { setPadding(dp(3), dp(9), dp(3), 0) })
        val information = card()
        header(information, null, "Emergency SMS can be sent while this phone is locked. Opening apps and optional WhatsApp drafts still requires an unlocked phone and the visible bubble or app.", "info")
        val disconnect = card(red = true)
        action(disconnect, "Disconnect and stop receiving", "disconnect", red = true) { stopService(Intent(this, ReceiverService::class.java)); updateState("Receiving is off") }
        val test = card()
        header(test, "First test", "Try: \"open Chrome on my other phone.\"\nEmergency SMS uses this phone's SIM and may incur carrier charges. Bluetooth transfer does not need internet.", "bulb")
        if (allowed()) refresh()
        syncControls()
    }
    private fun label(value: String, size: Float, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(if (bold) white else muted); setLineSpacing(dp(3).toFloat(), 1f)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private fun background(red: Boolean = false, bright: Boolean = false) = GradientDrawable(GradientDrawable.Orientation.TL_BR,
        if (bright) intArrayOf(0xFF00CFFF.toInt(), 0xFF135AFF.toInt(), 0xFF9923FF.toInt()) else if (red) intArrayOf(0xE0220B21.toInt(), 0xE00F091B.toInt()) else intArrayOf(0xE0071A32.toInt(), 0xE0030A18.toInt())).apply {
        cornerRadius = dp(23).toFloat(); setStroke(dp(1), if (red) 0xFFFF3659.toInt() else 0xFF0B528A.toInt())
    }
    private fun card(red: Boolean = false) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(14), dp(15), dp(14)); background = background(red)
        content.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
    }
    private fun header(parent: LinearLayout, title: String?, subtitle: String, icon: String) {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(LinkIcon(this, icon), LinearLayout.LayoutParams(dp(40), dp(40)))
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        if (title != null) texts.addView(label(title, 16f, true))
        texts.addView(label(subtitle, 13f).apply { setPadding(0, dp(3), 0, 0) })
        parent.addView(row)
    }
    private fun action(parent: LinearLayout, text: String, icon: String, bright: Boolean = false, red: Boolean = false, block: () -> Unit) {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL; setPadding(dp(12), dp(12), dp(12), dp(12))
            background = RippleDrawable(ColorStateList.valueOf(0x4456AAFF), background(red, bright), null)
            isClickable = true; isFocusable = true; contentDescription = text; setOnClickListener { block() }
        }
        row.addView(LinkIcon(this, icon, badge = false, red = red), LinearLayout.LayoutParams(dp(25), dp(25)))
        row.addView(label(text, 13f, true).apply { setTextColor(if (red) 0xFFFF4665.toInt() else white); setPadding(dp(10), 0, 0, 0); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, -2, 1f))
        parent.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = if (red) 0 else dp(10) })
    }
    private fun toggle(parent: LinearLayout, title: String, subtitle: String, icon: String, block: (Boolean) -> Unit): Switch {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(7), 0, dp(3)) }
        row.addView(LinkIcon(this, icon), LinearLayout.LayoutParams(dp(39), dp(39)))
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(6), 0) }
        texts.addView(label(title, 15f, true)); texts.addView(label(subtitle, 12f).apply { setPadding(0, dp(3), 0, 0) })
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        val control = Switch(this).apply {
            contentDescription = title
            thumbTintList = ColorStateList.valueOf(Color.WHITE)
            trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(0xFF008AFF.toInt(), 0xFF3B4764.toInt()))
            setOnCheckedChangeListener { _, checked -> if (!updating) block(checked) }
        }
        row.addView(control); parent.addView(row); return control
    }
    private fun beginReceiving() {
        val address = addresses.getOrNull(devices.selectedItemPosition)
        if (address == null) { updateState("Pair your Vivo, then refresh the list."); syncControls(); return }
        if (!allowed() || !smsAllowed() || (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)) {
            permissions(); syncControls(); return
        }
        prefs.edit().putString("controller", address).apply()
        try { startForegroundService(Intent(this, ReceiverService::class.java)); detail.text = "Starting receiving..." }
        catch (e: Exception) { updateState(e.message ?: "Could not enable receiving."); syncControls() }
    }
    private fun syncControls() {
        if (!::receiving.isInitialized) return
        updating = true
        receiving.isChecked = ReceiverService.instance != null
        devices.isEnabled = ReceiverService.instance == null
        bubble.isChecked = prefs.getBoolean("bubble", false)
        updating = false
    }
    private fun updateState(value: String) {
        val active = ReceiverService.instance != null
        val connected = ReceiverService.instance?.connected == true
        status.text = when {
            connected -> "Connected"
            value.startsWith("Connection ended") -> "Connection ended"
            value.startsWith("Waiting") -> "Ready to receive"
            value == "Receiving is off" -> "Receiving is off"
            else -> "Ghost Link status"
        }
        detail.text = if (value == "Receiving is off") "Enable receiving for your approved phone." else value
        connectionIcon.active = connected
        connectionIcon.invalidate()
        syncControls()
        if (active && prefs.getBoolean("bubble", false) && !Settings.canDrawOverlays(this)) detail.text = "$value\nAllow display over other apps to show the bubble."
    }
    private fun allowed() = Build.VERSION.SDK_INT < 31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    private fun smsAllowed() = checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    private fun permissions() {
        val missing = mutableListOf<String>()
        if (!allowed()) missing.add(Manifest.permission.BLUETOOTH_CONNECT)
        if (!smsAllowed()) missing.add(Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.POST_NOTIFICATIONS)
        if (missing.isEmpty()) refresh() else requestPermissions(missing.toTypedArray(), 81)
    }
    private fun refresh() {
        try {
            val adapter = getSystemService(BluetoothManager::class.java)?.adapter ?: error("Bluetooth unavailable")
            check(adapter.isEnabled) { "Turn Bluetooth on first." }
            val paired = adapter.bondedDevices.sortedBy { it.name ?: it.address }
            addresses = paired.map { it.address }
            val names = paired.map { "${it.name ?: "Phone"}\n${it.address}" }.ifEmpty { listOf("No paired phones. Pair in Bluetooth settings.") }
            devices.adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, names) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = super.getView(position, convertView, parent).also {
                    (it as TextView).apply { setTextColor(white); textSize = 13f; setSingleLine(false); maxLines = 2 }
                }
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = super.getDropDownView(position, convertView, parent).also {
                    (it as TextView).apply { setTextColor(white); setBackgroundColor(0xFF091A31.toInt()); setPadding(dp(14), dp(12), dp(14), dp(12)); textSize = 14f }
                }
            }.apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            val saved = prefs.getString("controller", null)
            val selected = addresses.indexOf(saved); if (selected >= 0) devices.setSelection(selected)
        } catch (e: Exception) { updateState(e.message ?: "Unable to refresh paired phones.") }
    }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (code == 81) {
            if (allowed() && smsAllowed()) refresh()
            else updateState("Nearby devices and SMS permissions are needed for Ghost Link emergency receiving.")
        }
    }
    override fun onResume() {
        super.onResume(); visible = true
        ReceiverService.update = { updateState(it) }
        updateState(ReceiverService.instance?.state ?: "Receiving is off")
        ReceiverService.instance?.refreshBubble()
        if (::devices.isInitialized && allowed()) refresh()
    }
    override fun onPause() { visible = false; ReceiverService.update = null; ReceiverService.instance?.refreshBubble(); super.onPause() }
}
