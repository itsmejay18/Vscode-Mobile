package com.customvscode.mobile.projects

import com.customvscode.mobile.bridge.BridgeSecurity
import com.customvscode.mobile.runtime.RuntimeManager
import com.customvscode.mobile.storage.Workspace
import java.io.File

data class NewProjectRequest(
    val template: String,
    val name: String,
    val laravelVersion: String = "latest",
    val database: String = "sqlite",
    val installFrontend: Boolean = true,
    val initGit: Boolean = true
)

sealed interface ProjectProgress {
    data class Step(val label: String, val done: Boolean, val active: Boolean) : ProjectProgress
    data class Log(val line: String) : ProjectProgress
    data class Finished(val ok: Boolean, val detail: String) : ProjectProgress
}

/**
 * Real project creation. Laravel path shells out to the real
 * `composer create-project` (never generates fake skeletons).
 * Until runtimes are installed it stops early with a clear report.
 */
class ProjectManager(
    private val workspace: Workspace,
    private val runtimes: RuntimeManager
) {
    fun dirFor(name: String): File = File(workspace.projects, name)

    fun create(req: NewProjectRequest, onEvent: (ProjectProgress) -> Unit) {
        Thread {
            try {
                if (!BridgeSecurity.validProjectName(req.name)) {
                    onEvent(ProjectProgress.Finished(false, "Invalid project name. Use A-Z a-z 0-9 . _ -"))
                    return@Thread
                }
                val dest = dirFor(req.name)
                if (dest.exists()) {
                    onEvent(ProjectProgress.Finished(false, "Directory ${dest.name} already exists."))
                    return@Thread
                }
                if (req.template == "laravel") {
                    createLaravel(req, dest, onEvent)
                } else {
                    dest.mkdirs()
                    File(dest, "README.md").writeText("# ${req.name}\n")
                    if (req.initGit) runtimes.shell.exec(listOf("git", "init"), dest, 10000)
                    onEvent(ProjectProgress.Finished(true, "Created ${dest.absolutePath}"))
                }
            } catch (t: Exception) {
                onEvent(ProjectProgress.Finished(false, t.message ?: "create failed"))
            }
        }.also { it.isDaemon = true; it.start() }
    }

    private fun step(label: String, done: Boolean = false, active: Boolean = false) =
        ProjectProgress.Step(label, done, active)

    private fun createLaravel(req: NewProjectRequest, dest: File, onEvent: (ProjectProgress) -> Unit) {
        onEvent(step("Checking PHP / Composer / Git / Node / npm", active = true))
        val need = RuntimeManager.FRAMEWORK_DEPS["laravel"] ?: emptyList()
        val report = runtimes.requirementReport(need)
        val missing = report.filter { !it.value }.keys.toList()
        onEvent(ProjectProgress.Log("Requirement report: $report"))
        if (missing.isNotEmpty()) {
            onEvent(ProjectProgress.Finished(false, "Missing requirements: ${missing.joinToString()}. Install them in Development Environments first."))
            return
        }
        onEvent(step("Checking PHP / Composer / Git / Node / npm", done = true))
        onEvent(step("Creating project directory", active = true))
        dest.parentFile?.mkdirs()
        onEvent(step("Creating project directory", done = true))
        onEvent(step("Installing Laravel (composer create-project)", active = true))
        val pkg = if (req.laravelVersion == "latest" || req.laravelVersion.isBlank()) "laravel/laravel" else "laravel/laravel:${req.laravelVersion}"
        val r = runtimes.shell.exec(
            listOf("composer", "create-project", pkg, dest.absolutePath, "--prefer-dist", "--no-interaction"),
            workspace.projects, 1000L * 60 * 20
        )
        onEvent(ProjectProgress.Log(r.stdout.takeLast(4000)))
        if (r.stdout.isNotBlank() && r.stderr.isNotBlank()) onEvent(ProjectProgress.Log(r.stderr.takeLast(4000)))
        if (r.exitCode != 0 || !File(dest, "artisan").exists()) {
            onEvent(ProjectProgress.Finished(false, "Composer exited with code ${r.exitCode}. See log."))
            return
        }
        onEvent(step("Installing Laravel (composer create-project)", done = true))
        if (req.database == "sqlite") {
            try {
                File(dest, "database/database.sqlite").also { it.parentFile?.mkdirs(); if (!it.exists()) it.createNewFile() }
                onEvent(ProjectProgress.Log("SQLite file prepared."))
            } catch (t: Exception) { onEvent(ProjectProgress.Log("SQLite prep warning: ${t.message}")) }
        }
        if (req.installFrontend) {
            onEvent(step("Installing npm packages", active = true))
            val n = runtimes.shell.exec(listOf("npm", "install", "--no-audit", "--no-fund"), dest, 1000L * 60 * 15)
            onEvent(ProjectProgress.Log(n.stdout.takeLast(2000)))
            onEvent(step("Installing npm packages", done = n.exitCode == 0))
        }
        if (req.initGit) {
            runtimes.shell.exec(listOf("git", "init"), dest, 10000)
            onEvent(ProjectProgress.Log("git init done."))
        }
        val v = runtimes.shell.exec(listOf("php", "artisan", "--version"), dest, 30000)
        onEvent(ProjectProgress.Log((v.stdout + v.stderr).trim()))
        onEvent(ProjectProgress.Finished(v.exitCode == 0, if (v.exitCode == 0) "Laravel ready at ${dest.absolutePath}" else "Project created but `php artisan --version` failed."))
    }
}
