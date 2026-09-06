package com.customvscode.mobile.bridge

import android.webkit.JavascriptInterface
import com.customvscode.mobile.projects.NewProjectRequest
import com.customvscode.mobile.projects.ProjectManager
import com.customvscode.mobile.projects.ProjectProgress
import com.customvscode.mobile.storage.Workspace
import org.json.JSONArray
import java.io.File

class ProjectBridge(
    private val workspace: Workspace,
    private val projects: ProjectManager,
    private val onEvent: (String) -> Unit
) {
    @JavascriptInterface
    fun listProjects(): String {
        val arr = JSONArray()
        workspace.projects.listFiles()?.filter { it.isDirectory }?.forEach { arr.put(it.name) }
        return arr.toString()
    }

    @JavascriptInterface
    fun createProject(template: String, name: String): String {
        if (!BridgeSecurity.validProjectName(name)) return "ERR:invalid-name"
        projects.create(NewProjectRequest(template, name)) { ev ->
            val msg = when (ev) {
                is ProjectProgress.Step -> "STEP:${ev.label}:${ev.done}:${ev.active}"
                is ProjectProgress.Log -> "LOG:${ev.line.take(500)}"
                is ProjectProgress.Finished -> "DONE:${ev.ok}:${ev.detail}"
            }
            onEvent(msg)
        }
        return "OK:started"
    }

    @JavascriptInterface
    fun projectTree(name: String): String {
        val dir = File(workspace.projects, name)
        if (!dir.isDirectory) return "[]"
        val arr = JSONArray()
        dir.walkTopDown().maxDepth(3).filter { it != dir }.forEach {
            arr.put(dir.toPath().relativize(it.toPath()).toString().replace(File.separatorChar, '/'))
        }
        return arr.toString()
    }
}
