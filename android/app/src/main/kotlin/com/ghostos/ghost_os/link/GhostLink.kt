package com.ghostos.ghost_os.link

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ghostos.link.LinkProtocol
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID

/** Explicit actions cross the approved Bluetooth link; audio and contact books never do. All callbacks use main. */
object GhostLink {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var context: Context
    var changed: (() -> Unit)? = null
    var completed: ((String) -> Unit)? = null
    private var socket: BluetoothSocket? = null
    private var output: DataOutputStream? = null
    private var generation = 0
    private var pending: String? = null
    private var session = ""
    var status = "Not connected"; private set
    var devices = emptyList<Map<String, String>>(); private set
    val connected get() = output != null
    fun init(ctx: Context) { context = ctx.applicationContext }
    fun refresh() {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: error("Bluetooth is unavailable on this device.")
        check(adapter.isEnabled) { "Turn on Bluetooth and pair both phones in Android settings." }
        devices = adapter.bondedDevices.map { mapOf("address" to it.address, "name" to (it.name ?: it.address)) }.sortedBy { it["name"] }
        changed?.invoke()
    }
    fun connect(address: String) {
        refresh()
        require(devices.any { it["address"] == address }) { "Pair this phone in Android Bluetooth settings first." }
        disconnect()
        val epoch = generation
        status = "Connecting…"; changed?.invoke()
        Thread {
            var current: BluetoothSocket? = null
            try {
                val adapter = context.getSystemService(BluetoothManager::class.java).adapter
                current = adapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(LinkProtocol.SERVICE)
                val active = current
                main.post { if (epoch == generation) socket = active else active.close() }
                active.connect()
                val input = DataInputStream(active.inputStream)
                val hello = JSONObject(LinkProtocol.read(input))
                require(hello.getInt("version") == LinkProtocol.VERSION && hello.getString("type") == "hello")
                val token = hello.getString("session"); UUID.fromString(token)
                val writer = DataOutputStream(active.outputStream)
                main.post {
                    if (epoch == generation) {
                        session = token; output = writer
                        status = "Connected to ${devices.firstOrNull { it["address"] == address }?.get("name") ?: "Ghost Link"}"
                        changed?.invoke()
                    } else active.close()
                }
                while (true) {
                    val response = JSONObject(LinkProtocol.read(input))
                    val id = response.getString("id")
                    require(response.getString("type") == "result" && response.getString("session") == token)
                    val message = response.getString("message").take(600)
                    main.post { if (epoch == generation && pending == id) { pending = null; completed?.invoke(message); changed?.invoke() } }
                }
            } catch (_: Exception) {
                main.post { if (epoch == generation) disconnect("Connection ended. Any pending action has an unknown outcome; check the other phone before repeating it.") }
            } finally { runCatching { current?.close() } }
        }.start()
        main.postDelayed({ if (epoch == generation && !connected) disconnect("Connection timed out. Open Ghost Link and enable receiving on the other phone.") }, 20000)
    }
    fun openApp(name: String) {
        require(name.isNotBlank() && name.length <= 120) { "Use a shorter app name, boss." }
        val writer = output ?: error("Connect Ghost Link in Settings first.")
        check(pending == null) { "Wait for the other phone's result first." }
        val id = UUID.randomUUID().toString(); val epoch = generation
        pending = id
        val payload = JSONObject().put("version", LinkProtocol.VERSION).put("type", "command")
            .put("session", session).put("id", id).put("operation", "open_app").put("name", name).toString()
        Thread {
            try { LinkProtocol.write(writer, payload) }
            catch (_: Exception) { main.post { if (epoch == generation) disconnect("Sending failed. Check the other phone before repeating the action.") } }
        }.start()
        main.postDelayed({ if (epoch == generation && pending == id) disconnect("No result arrived. Check the other phone before repeating the action.") }, 15000)
    }
    fun prepareWhatsApp(phone: String, message: String) {
        require(phone.matches(Regex("\\+?[0-9]{7,15}"))) { "The trusted contact number is invalid." }
        require(message.isNotBlank() && message.length <= 1800) { "The emergency message is invalid." }
        send(JSONObject().put("operation", "remote_emergency").put("phone", phone).put("message", message))
    }
    private fun send(operation: JSONObject) {
        val writer = output ?: error("Connect Ghost Link in Settings first.")
        check(pending == null) { "Wait for the other phone's result first." }
        val id = UUID.randomUUID().toString(); val epoch = generation
        pending = id
        val payload = operation.put("version", LinkProtocol.VERSION).put("type", "command")
            .put("session", session).put("id", id).toString()
        Thread {
            try { LinkProtocol.write(writer, payload) }
            catch (_: Exception) { main.post { if (epoch == generation) disconnect("Sending failed. Check the other phone before repeating the action.") } }
        }.start()
        main.postDelayed({ if (epoch == generation && pending == id) disconnect("No result arrived. Check the other phone before repeating the action.") }, 15000)
    }
    fun disconnect(message: String = "Not connected") {
        generation++
        val wasPending = pending != null
        pending = null; output = null; session = ""
        runCatching { socket?.close() }; socket = null
        status = message; changed?.invoke()
        if (wasPending) completed?.invoke(if (message == "Not connected") "Disconnected. The pending action's outcome is unknown; check the other phone before repeating it." else message)
    }
    fun snapshot() = mapOf("status" to status, "connected" to connected, "devices" to devices, "busy" to (pending != null))
}
