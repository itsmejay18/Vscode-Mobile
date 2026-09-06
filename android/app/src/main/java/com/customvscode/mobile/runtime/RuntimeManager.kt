package com.customvscode.mobile.runtime

import android.content.Context
import com.customvscode.mobile.storage.Workspace

/**
 * Facade over LinuxEnvironment + Shell + Processes + Registry + Installer.
 * Editor UI talks only to this, never directly to ProcessBuilder.
 */
class RuntimeManager(context: Context, workspace: Workspace) {
    val workspace = workspace
    val linux = LinuxEnvironment(workspace)
    val procs = ProcessManager()
    val shell = ShellManager(workspace, linux, procs)
    val registry = RuntimeRegistry(context, workspace)
    val installer = RuntimeInstaller(context, workspace)

    fun listRuntimes(): List<RuntimeInfo> =
        registry.loadManifests().map { registry.statusOf(it) }

    fun versionOf(binary: String): String? {
        val cwd = workspace.projects.also { it.mkdirs() }
        return shell.toolVersion(binary, listOf("--version"), cwd)
            ?: shell.toolVersion(binary, listOf("-v"), cwd)
            ?: shell.toolVersion(binary, listOf("version"), cwd)
    }

    fun requirementReport(ids: List<String>): Map<String, Boolean> =
        ids.associateWith { id ->
            registry.installedVersion(id) != null ||
                versionOf(id) != null ||
                (id == "npm" && versionOf("npm") != null)
        }

    companion object {
        /** Dependency map for framework installs (single source of truth). */
        val FRAMEWORK_DEPS = mapOf(
            "laravel" to listOf("bash", "git", "php", "composer", "node", "npm")
        )
    }
}
