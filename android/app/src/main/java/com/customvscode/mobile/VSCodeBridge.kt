package com.customvscode.mobile

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.io.File

/**
 * Bridge between Monaco (JS) and Kotlin.
 * JS calls: AndroidBridge.onLog(msg), AndroidBridge.onSave(content)
 * Kotlin calls: webView.evaluateJavascript("loadCode(`...`)")
 */
class VSCodeBridge(
    private val webView: WebView,
    private val filesDir: File,
    private val onSave: (String) -> Unit,
    private val onLog: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onLog(msg: String) {
        handler.post { onLog(msg) }
    }

    @JavascriptInterface
    fun onSave(content: String) {
        try {
            File(filesDir, "last_edit.txt").writeText(content)
        } catch (_: Exception) { }
        handler.post { onSave(content) }
    }

    @JavascriptInterface
    fun getLastEdit(): String {
        return try {
            File(filesDir, "last_edit.txt").readText()
        } catch (_: Exception) {
            "// Welcome to Custom VSCode Mobile\n// Open a repo file or paste code here.\n"
        }
    }

    fun loadCode(code: String, language: String = "javascript") {
        val esc = code
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")
        handler.post {
            webView.evaluateJavascript(
                "loadCode(`$esc`, `$language`)",
                null
            )
        }
    }
}
