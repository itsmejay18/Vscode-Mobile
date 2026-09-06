package com.customvscode.mobile.runtime

import com.customvscode.mobile.storage.Workspace
import java.io.File

/**
 * Local shell execution against the workspace.
 * Uses /system/bin/sh on-device (Bash runtime upgrades this to real bash
 * once the bash runtime package is installed).
 * All commands run with cwd inside the workspace; callers must validate.
 */
class ShellManager(
    private val workspace: Workspace,
    private val linux: LinuxEnvironment,
    private val procs: ProcessManager
) {
    data class Result(val exitCode: Int, val stdout: String, val stderr: String)

    fun shell(): String {
        val bashBin = File(File(workspace.runtimes, "bash"), "bin/bash")
        if (bashBin.canExecute()) return bashBin.absolutePath
        for (c in listOf("/system/bin/sh", "/bin/sh", "sh")) {
            try {
                val p = ProcessBuilder(c, "-c", "echo ok").start()
                val out = p.inputStream.bufferedReader().readText()
                p.waitFor()
                if (out.contains("ok")) return c
            } catch (_: Exception) { }
        }
        return "/system/bin/sh"
    }

    fun exec(cmd: List<String>, cwd: File, timeoutMs: Long = 15000): Result {
        return try {
            val e = procs.start(cmd, cwd, linux.envPathExtra())
            val out = e.process.inputStream.bufferedReader().readText()
            val err = e.process.errorStream.bufferedReader().readText()
            val deadline = System.currentTimeMillis() + timeoutMs
            while (e.exitCode == null && System.currentTimeMillis() < deadline) Thread.sleep(50)
            if (e.exitCode == null) { procs.stop(e.id); Result(124, out, err + "\n[timed out]") }
            else Result(e.exitCode ?: 1, out, err)
        } catch (t: Exception) {
            Result(1, "", t.message ?: "exec failed")
        }
    }

    fun execScript(script: String, cwd: File, timeoutMs: Long = 15000): Result =
        exec(listOf(shell(), "-c", script), cwd, timeoutMs)

    fun toolVersion(binary: String, args: List<String>, cwd: File): String? {
        val r = exec(listOf(binary) + args, cwd, 8000)
        if (r.exitCode != 0) return null
        return (r.stdout + r.stderr).lineSequence().firstOrNull { it.isNotBlank() }?.trim()
    }
}
