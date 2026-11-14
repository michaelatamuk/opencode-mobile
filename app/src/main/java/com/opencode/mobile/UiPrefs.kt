package com.opencode.mobile

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/** Appearance preferences, shared between the theme wrapper and Settings. */
object UiPrefs {

    private const val FILE = "opencode_ui"
    private const val MODE = "theme_mode"
    private const val ACCENT = "accent"
    private const val SHOW_SUBAGENTS = "show_subagents"

    val mode = MutableStateFlow("dynamic")
    val accent = MutableStateFlow("blue")
    val showSubagents = MutableStateFlow(false)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        mode.value = prefs.getString(MODE, "dynamic") ?: "dynamic"
        accent.value = prefs.getString(ACCENT, "blue") ?: "blue"
        showSubagents.value = prefs.getBoolean(SHOW_SUBAGENTS, false)
    }

    fun setShowSubagents(context: Context, value: Boolean) {
        showSubagents.value = value
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(SHOW_SUBAGENTS, value).apply()
    }

    fun setMode(context: Context, value: String) {
        mode.value = value
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(MODE, value).apply()
    }

    fun setAccent(context: Context, value: String) {
        accent.value = value
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(ACCENT, value).apply()
    }
}
