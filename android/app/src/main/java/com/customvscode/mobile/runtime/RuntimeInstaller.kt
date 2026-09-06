package com.customvscode.mobile.runtime

import android.content.Context
import com.customvscode.mobile.storage.Workspace
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads + installs runtime packages.
 * Stage 1: honest stub — verifies connectivity, writes a log, and
 * reports NOT_SUPPORTED until per-ABI package URLs are published.
 * UI must show "Not Installed" and never pretend success.
 */
class RuntimeInstaller(
    private val context: Context,
    private val workspace: Workspace
) {
    interface Listener {
        fun onProgress(id: String, msg: String)
        fun onDone(id: String, ok: Boolean, detail: String)
    }

    fun install(id: String, listener: Listener) {
        Thread {
            try {
                listener.onProgress(id, "Checking connectivity…")
                val online = try {
                    val c = (URL("https://api.github.com").openConnection() as HttpURLConnection)
                    c.connectTimeout = 8000; c.connect()
                    val ok = c.responseCode < 500; c.disconnect(); ok
                } catch (_: Exception) { false }
                if (!online) {
                    listener.onDone(id, false, "No internet. Runtime downloads need internet.")
                    return@Thread
                }
                val logDir = workspace.logs.also { it.mkdirs() }
                File(logDir, "runtime-$id.log").appendText("${System.currentTimeMillis()} install($id): package hosting not configured yet (TODO Stage 6+)\n")
                listener.onDone(
                    id, false,
                    "Runtime package hosting not configured yet. Manifest parsed, downloader ready, binaries pending (TODO)."
                )
            } catch (t: Exception) {
                listener.onDone(id, false, t.message ?: "install failed")
            }
        }.also { it.isDaemon = true; it.start() }
    }
}
