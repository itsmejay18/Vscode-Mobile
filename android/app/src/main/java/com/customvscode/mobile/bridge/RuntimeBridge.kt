package com.customvscode.mobile.bridge

import android.webkit.JavascriptInterface
import com.customvscode.mobile.runtime.RuntimeManager
import com.customvscode.mobile.runtime.RuntimeStatus
import org.json.JSONArray
import org.json.JSONObject

class RuntimeBridge(private val runtimes: RuntimeManager) {

    @JavascriptInterface
    fun listRuntimes(): String {
        val arr = JSONArray()
        runtimes.listRuntimes().forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.id)
                    .put("status", it.status.name)
                    .put("version", it.version ?: "")
                    .put("installDir", it.installDir ?: "")
            )
        }
        return arr.toString()
    }

    @JavascriptInterface
    fun installRuntime(id: String): String {
        var result = "STARTED"
        runtimes.installer.install(id, object : com.customvscode.mobile.runtime.RuntimeInstaller.Listener {
            override fun onProgress(pid: String, msg: String) { result = "PROGRESS:$msg" }
            override fun onDone(pid: String, ok: Boolean, detail: String) { result = "DONE:$ok:$detail" }
        })
        return "OK:install-started (see Development Environments for status)"
    }

    @JavascriptInterface
    fun runtimeVersion(binary: String): String =
        runtimes.versionOf(binary) ?: "NOT_INSTALLED:${RuntimeStatus.NOT_INSTALLED.name}"
}
