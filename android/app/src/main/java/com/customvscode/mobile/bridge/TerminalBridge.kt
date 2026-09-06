package com.customvscode.mobile.bridge

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.customvscode.mobile.runtime.RuntimeManager
import com.customvscode.mobile.storage.Workspace
import com.customvscode.mobile.terminal.TerminalSession
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Terminal sessions: create/write/resize/interrupt/close. Output pushed to JS. */
class TerminalBridge(
    private val webView: WebView,
    private val workspace: Workspace,
    private val runtimes: RuntimeManager
) {
    private val handler = Handler(Looper.getMainLooper())
    private val sessions = ConcurrentHashMap<Long, TerminalSession>()
    private val ids = AtomicLong(1)

    private fun js(expr: String) {
        handler.post { try { webView.evaluateJavascript(expr, null) } catch (_: Exception) { } }
    }

    @JavascriptInterface
    fun createSession(cwdRel: String): Long {
        val cwd = BridgeSecurity.resolve(workspace.root, cwdRel.ifEmpty { "projects" })
            ?: workspace.projects
        cwd.mkdirs()
        val s = TerminalSession(workspace, runtimes.linux, runtimes.procs, cwd)
        val id = ids.getAndIncrement()
        s.onOutput = { chunk ->
            val esc = chunk.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
            js("window.__termData && window.__termData($id, `$esc`)")
        }
        return if (s.start()) { sessions[id] = s; id } else -1
    }

    @JavascriptInterface
    fun write(id: Long, data: String) { sessions[id]?.write(data) }

    @JavascriptInterface
    fun interrupt(id: Long) { sessions[id]?.interrupt() }

    @JavascriptInterface
    fun resize(id: Long, cols: Int, rows: Int) { /* PTY resize: TODO with native pty */ }

    @JavascriptInterface
    fun closeSession(id: Long) { sessions.remove(id)?.close() }
}
