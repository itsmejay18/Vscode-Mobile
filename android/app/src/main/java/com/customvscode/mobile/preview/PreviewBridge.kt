package com.customvscode.mobile.preview

import android.webkit.JavascriptInterface

class PreviewBridge(private val previews: PreviewManager) {
    @JavascriptInterface
    fun startLaravel(project: String): String = previews.startLaravel(project)

    @JavascriptInterface
    fun stop(): String { previews.stop(); return "OK:stopped" }

    @JavascriptInterface
    fun url(): String = previews.url()
}
