package com.customvscode.mobile.bridge

import java.io.File

/** Blocks directory traversal; all bridge paths must stay inside the workspace. */
object BridgeSecurity {
    fun resolve(workspaceRoot: File, requested: String): File? {
        return try {
            if (requested.contains('\u0000')) return null
            val base = workspaceRoot.canonicalFile
            val target = File(base, requested).canonicalFile
            if (!target.path.startsWith(base.path)) return null
            target
        } catch (_: Exception) { null }
    }

    fun validProjectName(name: String): Boolean {
        if (name.isBlank() || name.length > 64) return false
        return name.matches(Regex("[A-Za-z0-9._-]+"))
    }
}
