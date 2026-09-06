package com.customvscode.mobile.runtime

import com.customvscode.mobile.storage.Workspace
import java.io.File

/**
 * Tracks child processes (php artisan serve, npm run dev, composer, ...).
 * Each entry keeps command, cwd, status, start time; kill supported.
 */
class ProcessManager {
    data class Entry(
        val id: Long,
        val command: List<String>,
        val cwd: File,
        val process: Process,
        val startedAt: Long,
        var endedAt: Long? = null,
        var exitCode: Int? = null
    )

    private val lock = Any()
    private var nextId = 1L
    private val entries = mutableMapOf<Long, Entry>()

    fun start(command: List<String>, cwd: File, extraPath: String?): Entry {
        val pb = ProcessBuilder(command).directory(cwd).redirectErrorStream(false)
        val env = pb.environment()
        if (!extraPath.isNullOrEmpty()) {
            env["PATH"] = "$extraPath:${env["PATH"]}"
        }
        env["TERM"] = "xterm-256color"
        val proc = pb.start()
        val e = Entry(synchronized(lock) { nextId++ }, command, cwd, proc, System.currentTimeMillis())
        synchronized(lock) { entries[e.id] = e }
        Thread {
            val code = try { proc.waitFor() } catch (_: Exception) { -1 }
            synchronized(lock) {
                e.exitCode = code
                e.endedAt = System.currentTimeMillis()
            }
        }.also { it.isDaemon = true; it.start() }
        return e
    }

    fun list(): List<Entry> = synchronized(lock) { entries.values.toList() }

    fun stop(id: Long) {
        synchronized(lock) { entries[id] }?.process?.destroy()
        Thread {
            Thread.sleep(2000)
            synchronized(lock) { entries[id] }?.process?.destroyForcibly()
        }.also { it.isDaemon = true; it.start() }
    }

    fun stopAllIn(cwd: File) {
        list().filter { it.cwd == cwd && it.exitCode == null }.forEach { stop(it.id) }
    }
}
