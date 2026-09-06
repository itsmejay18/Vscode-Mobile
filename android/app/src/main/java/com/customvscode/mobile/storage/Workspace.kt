package com.customvscode.mobile.storage

import java.io.File

/**
 * Scoped-storage compliant workspace:
 * <filesDir>/CustomVSCode/{projects,runtimes,packages,cache,settings,logs}
 */
class Workspace(root: File) {
    val root = File(root, "CustomVSCode")
    val projects = File(root, "projects")
    val runtimes = File(root, "runtimes")
    val packages = File(root, "packages")
    val cache = File(root, "cache")
    val settings = File(root, "settings")
    val logs = File(root, "logs")

    fun ensure() {
        listOf(root, projects, runtimes, packages, cache, settings, logs)
            .forEach { it.mkdirs() }
    }
}
