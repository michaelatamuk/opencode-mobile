package com.opencode.mobile

import android.content.Context

/**
 * Local "last seen" marker per session. The server's `POST /view` does not move
 * `time.viewed`, so unread state is tracked on the device instead.
 */
class SeenStore(context: Context) {
    private val prefs = SecurePrefs.get(context)

    fun lastSeen(sessionID: String): Long = prefs.getLong("seen_$sessionID", 0L)

    fun markSeen(sessionID: String, updated: Long) {
        if (updated <= 0L) return
        prefs.edit().putLong("seen_$sessionID", updated).apply()
    }
}
