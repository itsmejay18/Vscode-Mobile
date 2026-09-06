package com.customvscode.mobile.preview

import com.customvscode.mobile.runtime.RuntimeManager
import com.customvscode.mobile.storage.Workspace
import java.io.File
import java.net.ServerSocket

/**
 * Runs `php artisan serve --host=127.0.0.1 --port=N` inside the project dir
 * via ProcessManager. Preview WebView points at http://127.0.0.1:N.
 * Localhost only; never a remote server.
 */
class PreviewManager(
    private val workspace: Workspace,
    private val runtimes: RuntimeManager
) {
    private var serveId: Long? = null
    var port: Int = 8000
        private set

    fun freePort(): Int {
        return try {
            ServerSocket(0).use { it.localPort }
        } catch (_: Exception) { 8000 }
    }

    fun startLaravel(projectName: String): String {
        val dir = File(workspace.projects, projectName)
        if (!File(dir, "artisan").exists()) return "ERR:no-artisan"
        port = freePort()
        val e = runtimes.procs.start(
            listOf("php", "artisan", "serve", "--host=127.0.0.1", "--port=$port"),
            dir, runtimes.linux.envPathExtra()
        )
        serveId = e.id
        return "OK:http://127.0.0.1:$port"
    }

    fun stop() {
        serveId?.let { try { runtimes.procs.stop(it) } catch (_: Exception) { } }
        serveId = null
    }

    fun url(): String = "http://127.0.0.1:$port"
}
