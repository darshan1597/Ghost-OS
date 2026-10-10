package com.ghostos.link.receiver

import android.app.*
import android.bluetooth.*
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.telephony.SmsManager
import com.ghostos.link.LinkProtocol
import com.ghostos.link.LinkRequestGuard
import com.ghostos.ghost_os.core.AppMatcher
import com.ghostos.ghost_os.core.AppMatch
import com.ghostos.ghost_os.core.InstalledApp
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID

class ReceiverService : Service() {
    companion object { var instance: ReceiverService? = null; var update: ((String) -> Unit)? = null }
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    @Volatile private var server: BluetoothServerSocket? = null
    @Volatile private var socket: BluetoothSocket? = null
    private var approved = ""
    private var bubble: LinkBubble? = null
    var connected = false; private set
    var processing = false; private set
    override fun onCreate() { super.onCreate(); bubble = LinkBubble(this) }
    var state = "Receiving is off"; private set
    private fun status(value: String) { state = value; update?.invoke(value); refreshBubble() }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "stop") { stopSelf(); return START_NOT_STICKY }
        if (running) { status("Already receiving. Stop before changing the approved controller."); return START_NOT_STICKY }
        approved = getSharedPreferences("link", MODE_PRIVATE).getString("controller", "") ?: ""
        if (approved.isBlank()) { stopSelf(); return START_NOT_STICKY }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("link", "Ghost Link receiving", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, LinkActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, ReceiverService::class.java).setAction("stop"), PendingIntent.FLAG_IMMUTABLE)
        startForeground(41, Notification.Builder(this, "link").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Ghost Link receiving enabled").setContentText("Only the approved paired phone can open apps.")
            .setContentIntent(open).setOngoing(true).addAction(Notification.Action.Builder(null, "Disconnect", stop).build()).build())
        instance = this; running = true; refreshBubble(); status("Waiting for your approved Vivo…")
        Thread { receive() }.start()
        return START_NOT_STICKY
    }
    private fun receive() {
        try {
            val adapter = getSystemService(BluetoothManager::class.java).adapter
            check(adapter.isEnabled)
            while (running) {
                val listener = adapter.listenUsingRfcommWithServiceRecord("Ghost Link", LinkProtocol.SERVICE)
                server = listener
                val peer = listener.accept(); server = null; listener.close()
                if (!running || peer.remoteDevice.address != approved || peer.remoteDevice.bondState != BluetoothDevice.BOND_BONDED) {
                    peer.close(); continue
                }
                socket = peer
                try {
                    val session = UUID.randomUUID().toString()
                    val guard = LinkRequestGuard()
                    val input = DataInputStream(peer.inputStream); val output = DataOutputStream(peer.outputStream)
                    LinkProtocol.write(output, JSONObject().put("version", LinkProtocol.VERSION).put("type", "hello").put("session", session).toString())
                    main.post { if (running) { connected = true; status("Connected to approved Vivo") } }
                    while (running) {
                        val request = JSONObject(LinkProtocol.read(input))
                        require(peer.remoteDevice.bondState == BluetoothDevice.BOND_BONDED && peer.remoteDevice.address == approved)
                        require(request.getInt("version") == LinkProtocol.VERSION && request.getString("type") == "command" && request.getString("session") == session)
                        val id = request.getString("id"); val operation = request.getString("operation")
                        val name = request.optString("name"); val phone = request.optString("phone"); val message = request.optString("message")
                        require(guard.accept(id, operation, name, phone, message))
                        val receivedAt = SystemClock.elapsedRealtime()
                        val done = java.util.concurrent.CountDownLatch(1)
                        var result = "Request expired; give a fresh command."
                        main.post {
                            try {
                                processing = true; refreshBubble()
                                if (running && socket === peer && SystemClock.elapsedRealtime() - receivedAt < 5000) {
                                    result = when (operation) {
                                        "open_app" -> openApp(name)
                                        "remote_emergency" -> deliverEmergency(phone, message)
                                        else -> "Unsupported request."
                                    }
                                }
                            } catch (_: Exception) { result = "Android refused the app launch. Open Ghost Link and try a fresh command." }
                            finally { processing = false; refreshBubble(); done.countDown() }
                        }
                        done.await()
                        LinkProtocol.write(output, JSONObject().put("type", "result").put("session", session).put("id", id).put("message", result).toString())
                    }
                } catch (_: Exception) { /* Invalid frames or lost connections close this session. No retries. */ }
                finally { peer.close(); socket = null }
                main.post { connected = false; processing = false; if (running) status("Connection ended. Waiting for approved Vivo…") }
            }
        } catch (_: Exception) { main.post { if (running) { status("Bluetooth receiving failed. Turn Bluetooth on and enable receiving again."); stopSelf() } } }
    }
    private fun openApp(query: String): String {
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked) return "The other phone is locked. Unlock it and give a fresh command."
        if (!getSystemService(PowerManager::class.java).isInteractive) return "Wake the other phone and give a fresh command."
        if (!LinkActivity.visible && (bubble?.visible != true || !Settings.canDrawOverlays(this))) return "Open Ghost Link on the other phone or enable its visible connection bubble, then give a fresh command."
        val apps = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { InstalledApp(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }.distinctBy { it.packageName }
        val app = when (val match = AppMatcher.match(query, apps)) {
            AppMatch.Missing -> return "No app named $query was found on the other phone."
            is AppMatch.Ambiguous -> return "More than one app matches on the other phone: ${match.apps.take(4).joinToString { it.name }}. Say its full name."
            is AppMatch.Found -> match.app
        }
        val launch = packageManager.getLaunchIntentForPackage(app.packageName) ?: return "That app is no longer available on the other phone."
        startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        status("Requested ${app.name}")
        return "Asked Android to open ${app.name} on the other phone."
    }
    private fun deliverEmergency(phone: String, message: String): String {
        if (checkSelfPermission(android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return "Ghost Link needs SMS permission on the other phone. Open it and enable receiving again."
        }
        try {
            @Suppress("DEPRECATION") val sms = SmsManager.getDefault()
            sms.sendMultipartTextMessage(phone, null, sms.divideMessage(message), null, null)
        } catch (_: Exception) { return "The other phone could not send the emergency SMS. Check its SIM and mobile service." }
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked || !getSystemService(PowerManager::class.java).isInteractive) {
            return "Emergency SMS sent from the other phone. Its screen is locked, so the optional WhatsApp draft was not opened."
        }
        if (!LinkActivity.visible && (bubble?.visible != true || !Settings.canDrawOverlays(this))) {
            return "Emergency SMS sent from the other phone. Open Ghost Link or enable its bubble to also open WhatsApp drafts."
        }
        val uri = Uri.Builder().scheme("https").authority("wa.me").appendPath(phone.removePrefix("+"))
            .appendQueryParameter("text", message).build()
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            startActivity(intent); status("Emergency WhatsApp draft opened")
            "Emergency SMS sent from the other phone. The optional WhatsApp draft is also ready; review it and press Send."
        } catch (_: Exception) { "Emergency SMS sent from the other phone. WhatsApp could not be opened there." }
    }
    fun refreshBubble() { if (running) bubble?.refresh() }
    override fun onDestroy() {
        running = false; connected = false; processing = false; instance = null
        runCatching { server?.close() }; runCatching { socket?.close() }
        bubble?.dispose(); bubble = null; status("Receiving is off"); super.onDestroy()
    }
}
