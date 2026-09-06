package com.customvscode.mobile

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.customvscode.mobile.bridge.FileBridge
import com.customvscode.mobile.bridge.ProjectBridge
import com.customvscode.mobile.bridge.RuntimeBridge
import com.customvscode.mobile.bridge.TerminalBridge
import com.customvscode.mobile.preview.PreviewBridge
import com.customvscode.mobile.webview.WorkbenchWebView

/**
 * Stage 1 host: native toolbar (Project / Run / Preview / Settings)
 * + local WorkbenchWebView. No remote editor, no cloud dependency.
 */
class MainActivity : Activity() {

    private lateinit var app: IdeApplication
    private lateinit var web: WorkbenchWebView

    @SuppressLint("AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as IdeApplication

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(-1, -1)
        }

        val barScroll = android.widget.HorizontalScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(0xFF1E1E1E.toInt())
            isHorizontalScrollBarEnabled = false
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
        }
        fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
        fun btn(label: String, fn: () -> Unit) =
            android.widget.Button(this).apply {
                text = label
                textSize = 14f
                minimumHeight = dp(48)
                minHeight = dp(48)
                setPadding(dp(16), 0, dp(16), 0)
                setOnClickListener { fn() }
            }.also {
                bar.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)).apply {
                    marginEnd = dp(8)
                })
            }

        btn("＋ Project") { newProjectDialog() }
        btn("▶ Run") { runLaravelDialog() }
        btn("🌐 Preview") { openPreview() }
        btn("↻ Reload") { web.loadWorkbench() }
        barScroll.addView(bar)

        web = WorkbenchWebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        web.addJavascriptInterface(FileBridge(app.workspace), "FileBridge")
        web.addJavascriptInterface(TerminalBridge(web, app.workspace, app.runtimes), "TerminalBridge")
        web.addJavascriptInterface(
            ProjectBridge(app.workspace, app.projects) { msg ->
                runOnUiThread { web.pushEvent("window.__projectEvent", msg) }
            },
            "ProjectBridge"
        )
        web.addJavascriptInterface(RuntimeBridge(app.runtimes), "RuntimeBridge")
        web.addJavascriptInterface(PreviewBridge(app.previews), "PreviewBridge")
        // Legacy bridge kept for GitHub prototype screen (TODO: remove after explorer ships)
        try {
            web.addJavascriptInterface(
                VSCodeBridge(web, filesDir,
                    onSave = { },
                    onLog = { }),
                "AndroidBridge"
            )
        } catch (_: Exception) { }

        root.addView(barScroll)
        root.addView(web)
        setContentView(root)
        web.loadWorkbench()
    }

    private fun newProjectDialog() {
        val input = EditText(this).apply { hint = "project-name (a-z 0-9 . _ -)" }
        AlertDialog.Builder(this)
            .setTitle("Create Laravel Project")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                web.evaluateJavascript("window.__createProject && window.__createProject(`laravel`, `$name`)", null)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun runLaravelDialog() {
        val input = EditText(this).apply { hint = "project-name" }
        AlertDialog.Builder(this)
            .setTitle("Run: php artisan serve")
            .setView(input)
            .setPositiveButton("Start") { _, _ ->
                val name = input.text.toString().trim()
                Thread {
                    val res = app.previews.startLaravel(name)
                    runOnUiThread {
                        Toast.makeText(this, res, Toast.LENGTH_LONG).show()
                        web.pushEvent("window.__projectEvent", "PREVIEW:$res")
                    }
                }.also { it.isDaemon = true; it.start() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openPreview() {
        val url = app.previews.url()
        Toast.makeText(this, url, Toast.LENGTH_SHORT).show()
        web.evaluateJavascript("window.__openPreview && window.__openPreview(`$url`)", null)
    }

    override fun onDestroy() {
        try {
            web.removeJavascriptInterface("FileBridge")
            web.destroy()
        } catch (_: Exception) { }
        super.onDestroy()
    }
}
