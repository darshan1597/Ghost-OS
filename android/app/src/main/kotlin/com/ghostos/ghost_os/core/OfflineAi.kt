package com.ghostos.ghost_os.core

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

/** Optional, user-downloaded GGUF model. The base APK never contains model weights. */
object OfflineAi {
    private const val FILE_NAME = "qwen3-0.6b-q4_0.gguf"
    private const val URL = "https://huggingface.co/ggml-org/Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B-Q4_0.gguf"
    private const val SHA256 = "da2572f16c06133561ce56accaa822216f2391ef4d37fba427801cd6736417d4"
    const val DOWNLOAD_BYTES = 429_000_000L
    private lateinit var context: Context
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var engine: InferenceEngine? = null
    private var verified = false
    private var checking = false
    private var busy = false
    var changed: (() -> Unit)? = null

    private val prefs get() = context.getSharedPreferences("offline_ai", Context.MODE_PRIVATE)
    private val manager get() = context.getSystemService(DownloadManager::class.java)
    private fun modelFile() = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.resolve(FILE_NAME)

    fun init(ctx: Context) { context = ctx.applicationContext; refresh() }
    fun enabled() = prefs.getBoolean("enabled", true)
    fun setEnabled(value: Boolean) { prefs.edit().putBoolean("enabled", value).apply(); changed?.invoke() }

    fun snapshot(): Map<String, Any?> {
        val id = prefs.getLong("downloadId", -1L)
        var downloaded = 0L; var total = DOWNLOAD_BYTES; var status = "notInstalled"
        if (verified && modelFile()?.isFile == true) status = "ready"
        else if (checking) status = "checking"
        else if (id >= 0) manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (cursor.moveToFirst()) {
                downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).takeIf { it > 0 } ?: total
                status = when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_PENDING -> "pending"
                    DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED -> "downloading"
                    DownloadManager.STATUS_SUCCESSFUL -> "checking"
                    DownloadManager.STATUS_FAILED -> "failed"
                    else -> "notInstalled"
                }
            }
        }
        return mapOf("enabled" to enabled(), "status" to status, "installed" to (status == "ready"),
            "busy" to busy, "downloaded" to downloaded, "total" to total, "modelMb" to 429)
    }

    fun download() {
        check(!verified) { "The offline AI is already installed." }
        cancelDownload()
        modelFile()?.delete()
        val request = DownloadManager.Request(Uri.parse(URL))
            .setTitle("Ghost offline AI")
            .setDescription("Downloading the optional private conversation model")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(false)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
        prefs.edit().putLong("downloadId", manager.enqueue(request)).apply()
        poll(); changed?.invoke()
    }

    fun cancelDownload() {
        val id = prefs.getLong("downloadId", -1L)
        if (id >= 0) manager.remove(id)
        prefs.edit().remove("downloadId").apply(); changed?.invoke()
    }

    fun remove() {
        cancelDownload()
        runCatching { engine?.cleanUp() }
        engine = null; verified = false; modelFile()?.delete(); changed?.invoke()
    }

    fun refresh() {
        if (verified || checking) return
        val file = modelFile()
        if (file?.isFile == true) {
            checking = true; changed?.invoke()
            scope.launch {
                verified = sha256(file) == SHA256
                checking = false
                if (!verified) file.delete() else prefs.edit().remove("downloadId").apply()
                main.post { changed?.invoke() }
            }
        } else poll()
    }

    private fun poll() {
        if (!::context.isInitialized || prefs.getLong("downloadId", -1L) < 0) return
        main.removeCallbacksAndMessages(this)
        main.postDelayed({
            changed?.invoke()
            val status = snapshot()["status"]
            if (status == "checking") refresh() else if (status in listOf("pending", "downloading")) poll()
        }, 1200)
    }

    fun respond(text: String, full: Boolean, callback: (Result<String>) -> Unit): Boolean {
        if (!enabled() || !verified || busy) return false
        val path = modelFile()?.absolutePath ?: return false
        busy = true; changed?.invoke()
        scope.launch {
            val result = runCatching {
                val chat = engine ?: AiChat.getInferenceEngine(context).also { engine = it }
                chat.state.filter { it is InferenceEngine.State.Initialized || it is InferenceEngine.State.ModelReady || it is InferenceEngine.State.Error }.first()
                if (chat.state.value is InferenceEngine.State.Error) chat.cleanUp()
                if (chat.state.value is InferenceEngine.State.Initialized) {
                    chat.loadModel(path)
                    chat.setSystemPrompt("You are Ghost, a warm concise offline phone companion. Call the user boss occasionally. Never claim you performed phone actions. Use plain English. Keep answers under ${if (full) 70 else 30} words.")
                }
                chat.sendUserPrompt("$text /no_think", if (full) 96 else 48).toList().joinToString("")
                    .replace(Regex("(?s)<think>.*?</think>"), "").trim()
                    .ifEmpty { "I'm here, boss. Could you say that another way?" }
            }
            busy = false
            main.post { changed?.invoke(); callback(result) }
        }
        return true
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
