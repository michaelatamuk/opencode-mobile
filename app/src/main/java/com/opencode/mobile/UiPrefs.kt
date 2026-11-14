package com.opencode.mobile

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/** Appearance preferences, shared between the theme wrapper and Settings. */
object UiPrefs {

    private const val FILE = "opencode_ui"
    private const val MODE = "theme_mode"
    private const val ACCENT = "accent"

    val mode = MutableStateFlow("dynamic")
    val accent = MutableStateFlow("blue")

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        mode.value = prefs.getString(MODE, "dynamic") ?: "dynamic"
        accent.value = prefs.getString(ACCENT, "blue") ?: "blue"
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
