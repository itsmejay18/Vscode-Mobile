package com.customvscode.mobile.terminal

import com.customvscode.mobile.runtime.LinuxEnvironment
import com.customvscode.mobile.runtime.ProcessManager
import com.customvscode.mobile.storage.Workspace
import java.io.File
import java.util.concurrent.LinkedBlockingQueue

/**
 * Interactive shell session backed by a real local process
 * (/system/bin/sh now, bash runtime binary once installed).
 * Not a fake: bytes flow between WebView xterm and the child stdin/stdout.
 */
class TerminalSession(
    private val workspace: Workspace,
    private val linux: LinuxEnvironment,
    private val procs: ProcessManager,
    var cwd: File
) {
    val id: Long = System.currentTimeMillis()
    private var entry: ProcessManager.Entry? = null
    private val input = LinkedBlockingQueue<ByteArray>()
    private var pump: Thread? = null
    var onOutput: ((String) -> Unit)? = null

    fun start(): Boolean {
        return try {
            cwd.mkdirs()
            val sh = run {
                val b = File(File(workspace.runtimes, "bash"), "bin/bash")
                if (b.canExecute()) b.absolutePath else "/system/bin/sh"
            }
            entry = procs.start(listOf(sh, "-i"), cwd, linux.envPathExtra())
            val e = entry ?: return false
            pump = Thread {
                try {
                    val out = e.process.inputStream
                    val err = e.process.errorStream
                    val buf = ByteArray(4096)
                    while (e.exitCode == null) {
                        // drain stdin queue -> child
                        while (true) {
                            val chunk = input.poll() ?: break
                            try { e.process.outputStream.write(chunk); e.process.outputStream.flush() } catch (_: Exception) { }
                        }
                        val nOut = try { out.available() } catch (_: Exception) { -1 }
                        val nErr = try { err.available() } catch (_: Exception) { -1 }
                        if (nOut < 0 && nErr < 0) break
                        if (nOut > 0) {
                            val n = out.read(buf, 0, minOf(buf.size, nOut))
                            if (n > 0) onOutput?.invoke(String(buf, 0, n))
                        }
                        if (nErr > 0) {
                            val n = err.read(buf, 0, minOf(buf.size, nErr))
                            if (n > 0) onOutput?.invoke(String(buf, 0, n))
                        }
                        if (nOut == 0 && nErr == 0) Thread.sleep(30)
                    }
                } catch (_: Exception) { }
            }.also { it.isDaemon = true; it.start() }
            // writer loop for queued input when child is idle
            Thread {
                while (entry?.exitCode == null) {
                    try {
                        val chunk = input.take()
                        try { entry?.process?.outputStream?.write(chunk); entry?.process?.outputStream?.flush() } catch (_: Exception) { }
                    } catch (_: Exception) { break }
                }
            }.also { it.isDaemon = true; it.start() }
            true
        } catch (_: Exception) { false }
    }

    fun write(data: String) {
        input.offer(data.toByteArray())
    }

    fun interrupt() {
        // Ctrl+C = ETX
        input.offer(byteArrayOf(0x03))
    }

    fun close() {
        try { entry?.let { procs.stop(it.id) } } catch (_: Exception) { }
        try { pump?.interrupt() } catch (_: Exception) { }
    }
}
