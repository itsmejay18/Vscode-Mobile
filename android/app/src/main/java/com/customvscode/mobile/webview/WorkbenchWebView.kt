package com.customvscode.mobile.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/** Local-only WebView: loads file:///android_asset/editor/workbench.html, no remote editor. */
@SuppressLint("SetJavaScriptEnabled")
class WorkbenchWebView(context: Context) : WebView(context) {
    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        webViewClient = WebViewClient()
    }

    fun loadWorkbench() {
        loadUrl("file:///android_asset/editor/workbench.html")
    }

    fun pushEvent(fn: String, jsonArg: String) {
        val esc = jsonArg.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
        post { try { evaluateJavascript("$fn(`$esc`)", null) } catch (_: Exception) { } }
    }
}
