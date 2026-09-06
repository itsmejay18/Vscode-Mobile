package com.customvscode.mobile

import android.app.Application
import com.customvscode.mobile.runtime.RuntimeManager
import com.customvscode.mobile.projects.ProjectManager
import com.customvscode.mobile.preview.PreviewManager
import com.customvscode.mobile.storage.Workspace

class IdeApplication : Application() {
    lateinit var workspace: Workspace
    lateinit var runtimes: RuntimeManager
    lateinit var projects: ProjectManager
    lateinit var previews: PreviewManager

    override fun onCreate() {
        super.onCreate()
        workspace = Workspace(filesDir).also { it.ensure() }
        runtimes = RuntimeManager(this, workspace)
        projects = ProjectManager(workspace, runtimes)
        previews = PreviewManager(workspace, runtimes)
    }
}
