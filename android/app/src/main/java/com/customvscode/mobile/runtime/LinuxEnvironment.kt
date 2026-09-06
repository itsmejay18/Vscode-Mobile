package com.customvscode.mobile.runtime

import com.customvscode.mobile.storage.Workspace
import java.io.File

/**
 * Rootless Linux userspace abstraction.
 * Stage 1-5: reports honestly whether a proot-style env is bootstrapped.
 * Full bootstrap (proot + bionic userland + packages) is a later stage;
 * until then ShellManager runs against Android's own shell with workspace PATH.
 */
class LinuxEnvironment(private val workspace: Workspace) {

    fun isBootstrapped(): Boolean =
        File(workspace.runtimes, ".linux_bootstrapped").exists()

    fun rootFsDir(): File = File(workspace.runtimes, "linux-rootfs")

    fun envPathExtra(): String {
        // Runtimes install their bin/ here; prepended to PATH for shells.
        val parts = listOf("php", "composer", "node", "git", "bash")
            .map { File(File(workspace.runtimes, it), "bin").absolutePath }
        return parts.joinToString(":")
    }
}
