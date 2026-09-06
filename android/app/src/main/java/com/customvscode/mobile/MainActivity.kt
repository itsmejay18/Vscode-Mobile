package com.customvscode.mobile

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var bridge: VSCodeBridge
    private lateinit var statusView: TextView
    private lateinit var tokenInput: EditText
    private lateinit var repoInput: EditText
    private lateinit var pathInput: EditText

    private var lastSha: String? = null
    private var pendingSave: String? = null

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 8)
            setBackgroundColor(0xFF252526.toInt())
        }

        statusView = TextView(this).apply {
            text = "Not connected"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 12f
        }

        tokenInput = EditText(this).apply {
            hint = getString(R.string.hint_token)
            setHintTextColor(0xFF888888.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
        }
        repoInput = EditText(this).apply {
            hint = getString(R.string.hint_repo)
            setHintTextColor(0xFF888888.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
        }
        pathInput = EditText(this).apply {
            hint = getString(R.string.hint_path)
            setHintTextColor(0xFF888888.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            setText("README.md")
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val btnLoad = Button(this).apply { text = "Open" }
        val btnPush = Button(this).apply { text = "Push" }
        val btnLocal = Button(this).apply { text = "Reload editor" }
        btnRow.addView(btnLoad, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        btnRow.addView(btnPush, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        btnRow.addView(btnLocal, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        form.addView(statusView)
        form.addView(tokenInput)
        form.addView(repoInput)
        form.addView(pathInput)
        form.addView(btnRow)

        val scroll = ScrollView(this).apply {
            addView(form)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1f
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.allowFileAccess = true
            webViewClient = WebViewClient()
        }

        bridge = VSCodeBridge(
            webView = webView,
            filesDir = filesDir,
            onSave = { content ->
                pendingSave = content
                statusView.text = "Edited locally (${content.length} chars) — tap Push to send to GitHub"
            },
            onLog = { msg ->
                statusView.text = msg
            }
        )
        webView.addJavascriptInterface(bridge, "AndroidBridge")
        webView.loadUrl("file:///android_asset/monaco.html")

        btnLoad.setOnClickListener { openFromGitHub() }
        btnPush.setOnClickListener { pushToGitHub() }
        btnLocal.setOnClickListener { webView.reload() }

        root.addView(scroll)
        root.addView(webView)
        setContentView(root)
    }

    private fun openFromGitHub() {
        val token = tokenInput.text.toString().trim()
        val repo = repoInput.text.toString().trim()
        val path = pathInput.text.toString().trim()
        if (token.isEmpty() || repo.isEmpty() || path.isEmpty()) {
            toast("Fill token + repo + path first")
            return
        }
        statusView.text = "Loading $repo/$path…"
        thread {
            val file = GitHubApi.getFile(token, repo, path)
            runOnUiThread {
                if (file == null) {
                    statusView.text = "Load failed — check repo/branch/token"
                    toast("Load failed")
                } else {
                    lastSha = file.sha
                    pendingSave = file.text
                    bridge.loadCode(file.text, guessLang(path))
                    statusView.text = "Loaded $path (${file.text.length} chars)"
                }
            }
        }
    }

    private fun pushToGitHub() {
        val token = tokenInput.text.toString().trim()
        val repo = repoInput.text.toString().trim()
        val path = pathInput.text.toString().trim()
        val content = pendingSave
        if (token.isEmpty() || repo.isEmpty() || path.isEmpty() || content == null) {
            toast("Nothing to push — Open a file and edit first")
            return
        }
        statusView.text = "Pushing…"
        thread {
            val ok = GitHubApi.putFile(token, repo, path, content, "Update $path via mobile", lastSha)
            runOnUiThread {
                statusView.text = if (ok) "Pushed $path ✓" else "Push failed — check token/sha"
                toast(if (ok) "Pushed" else "Push failed")
            }
        }
    }

    private fun guessLang(path: String): String {
        return when {
            path.endsWith(".kt") -> "kotlin"
            path.endsWith(".java") -> "java"
            path.endsWith(".py") -> "python"
            path.endsWith(".json") -> "json"
            path.endsWith(".md") -> "markdown"
            path.endsWith(".xml") -> "xml"
            path.endsWith(".html") -> "html"
            path.endsWith(".css") -> "css"
            path.endsWith(".ts") -> "typescript"
            else -> "javascript"
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
