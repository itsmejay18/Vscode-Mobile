package com.customvscode.mobile.settings

import android.content.Context

/** Simple key-value settings in app-private storage (backed by SharedPreferences). */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("ide_settings", Context.MODE_PRIVATE)

    fun get(key: String, def: String): String = prefs.getString(key, def) ?: def
    fun set(key: String, value: String) { prefs.edit().putString(key, value).apply() }

    fun theme(): String = get("editor.theme", "vs-dark")
    fun fontSize(): Int = get("editor.fontSize", "14").toIntOrNull() ?: 14
    fun wordWrap(): Boolean = get("editor.wordWrap", "on") == "on"
    fun serverUrl(): String = get("preview.serverUrl", "")
}
