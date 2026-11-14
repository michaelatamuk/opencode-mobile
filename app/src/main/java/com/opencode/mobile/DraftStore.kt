package com.opencode.mobile

import android.content.Context

/**
 * Per-session composer drafts. Stored outside composition so leaving the chat,
 * rotating, or process death does not discard typed text.
 */
object DraftStore {

    private const val KEY = "draft_new"

    private fun key(sessionID: String?): String =
        if (sessionID.isNullOrBlank()) KEY else "draft_$sessionID"

    fun get(context: Context, sessionID: String?): String =
        SecurePrefs.get(context).getString(key(sessionID), "").orEmpty()

    /** Saves non-blank text; blank changes are ignored so a send failure keeps it. */
    fun save(context: Context, sessionID: String?, text: String) {
        if (text.isBlank()) return
        SecurePrefs.get(context).edit().putString(key(sessionID), text).apply()
    }

    fun clear(context: Context, sessionID: String?) {
        SecurePrefs.get(context).edit().remove(key(sessionID)).apply()
    }
}
