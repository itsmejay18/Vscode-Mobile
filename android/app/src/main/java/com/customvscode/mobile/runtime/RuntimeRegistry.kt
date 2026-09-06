package com.customvscode.mobile.runtime

import android.content.Context
import com.customvscode.mobile.storage.Workspace
import org.json.JSONObject
import java.io.File

/**
 * Reads runtime manifest JSON files from assets and tracks install state
 * by checking <workspace>/runtimes/<id>/VERSION.
 * No hardcoding of versions in UI; manifests are the source of truth.
 */
class RuntimeRegistry(private val context: Context, private val workspace: Workspace) {

    data class Manifest(
        val id: String,
        val name: String,
        val version: String,
        val downloadSizeMb: Double,
        val installSizeMb: Double,
        val binaries: List<String>
    )

    fun loadManifests(): List<Manifest> {
        val out = mutableListOf<Manifest>()
        try {
            val files = context.assets.list("runtime-manifests") ?: arrayOf()
            for (f in files) {
                if (!f.endsWith(".json")) continue
                val text = context.assets.open("runtime-manifests/$f").bufferedReader().use { it.readText() }
                val o = JSONObject(text)
                out.add(
                    Manifest(
                        id = o.getString("id"),
                        name = o.optString("name", o.getString("id")),
                        version = o.optString("version", "unknown"),
                        downloadSizeMb = o.optDouble("downloadSizeMb", 0.0),
                        installSizeMb = o.optDouble("installSizeMb", 0.0),
                        binaries = o.optJSONArray("binaries")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList()
                    )
                )
            }
        } catch (_: Exception) { }
        if (out.isEmpty()) {
            // Fallback when assets not yet copied: minimal known set, marked clearly.
            listOf("bash", "git", "php", "composer", "node", "npm", "laravel-installer").forEach {
                out.add(Manifest(it, it, "pending", 0.0, 0.0, emptyList()))
            }
        }
        return out
    }

    fun statusOf(manifest: Manifest): RuntimeInfo {
        val dir = File(workspace.runtimes, manifest.id)
        val verFile = File(dir, "VERSION")
        return if (verFile.exists()) {
            RuntimeInfo(
                manifest.id, manifest.name,
                try { verFile.readText().trim() } catch (_: Exception) { manifest.version },
                RuntimeStatus.INSTALLED, dir.absolutePath, null, null
            )
        } else {
            RuntimeInfo(manifest.id, manifest.name, null, RuntimeStatus.NOT_INSTALLED, null, null, null)
        }
    }

    fun installedVersion(id: String): String? {
        val v = File(File(workspace.runtimes, id), "VERSION")
        return if (v.exists()) try { v.readText().trim() } catch (_: Exception) { null } else null
    }
}
