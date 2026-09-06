package com.customvscode.mobile.bridge

import android.webkit.JavascriptInterface
import com.customvscode.mobile.storage.Workspace
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Explicit file API. Every path validated against workspace root. */
class FileBridge(private val workspace: Workspace) {

    private fun ok(f: File?): Boolean = f != null

    @JavascriptInterface
    fun listDir(rel: String): String {
        val dir = BridgeSecurity.resolve(workspace.root, rel.ifEmpty { "projects" })
        if (!ok(dir) || !dir!!.exists()) return "[]"
        val arr = JSONArray()
        dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))?.forEach {
            arr.put(JSONObject().put("name", it.name).put("dir", it.isDirectory).put("size", it.length()))
        }
        return arr.toString()
    }

    @JavascriptInterface
    fun readFile(rel: String): String {
        val f = BridgeSecurity.resolve(workspace.root, rel) ?: return ""
        return try { if (f.isFile && f.length() < 2_000_000) f.readText() else "" } catch (_: Exception) { "" }
    }

    @JavascriptInterface
    fun writeFile(rel: String, content: String): Boolean {
        val f = BridgeSecurity.resolve(workspace.root, rel) ?: return false
        return try {
            f.parentFile?.mkdirs()
            f.writeText(content)
            true
        } catch (_: Exception) { false }
    }

    @JavascriptInterface
    fun createFile(rel: String): Boolean {
        val f = BridgeSecurity.resolve(workspace.root, rel) ?: return false
        return try {
            if (f.exists()) false else { f.parentFile?.mkdirs(); f.createNewFile() }
        } catch (_: Exception) { false }
    }

    @JavascriptInterface
    fun createDirectory(rel: String): Boolean {
        val f = BridgeSecurity.resolve(workspace.root, rel) ?: return false
        return try { f.mkdirs() } catch (_: Exception) { false }
    }

    @JavascriptInterface
    fun deletePath(rel: String): Boolean {
        val f = BridgeSecurity.resolve(workspace.root, rel) ?: return false
        return try {
            if (f == workspace.root || f == workspace.projects) return false
            f.deleteRecursively()
        } catch (_: Exception) { false }
    }

    @JavascriptInterface
    fun renamePath(fromRel: String, toRel: String): Boolean {
        val from = BridgeSecurity.resolve(workspace.root, fromRel) ?: return false
        val to = BridgeSecurity.resolve(workspace.root, toRel) ?: return false
        return try { from.renameTo(to) } catch (_: Exception) { false }
    }
}
