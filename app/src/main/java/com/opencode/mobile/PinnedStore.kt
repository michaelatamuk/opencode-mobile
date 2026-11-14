package com.opencode.mobile

import android.content.Context

/** Sessions the user pinned to the top of the list. */
object PinnedStore {

    private const val KEY = "pinned_sessions"

    fun all(context: Context): Set<String> =
        SecurePrefs.get(context).getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun toggle(context: Context, sessionID: String): Set<String> {
        val prefs = SecurePrefs.get(context)
        val current = prefs.getStringSet(KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (!current.add(sessionID)) current.remove(sessionID)
        prefs.edit().putStringSet(KEY, current).apply()
        return current
    }
}

/** Sessions whose notifications the user silenced. */
object MutedStore {

    private const val KEY = "muted_sessions"

    fun all(context: Context): Set<String> =
        SecurePrefs.get(context).getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun toggle(context: Context, sessionID: String): Set<String> {
        val prefs = SecurePrefs.get(context)
        val current = prefs.getStringSet(KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (!current.add(sessionID)) current.remove(sessionID)
        prefs.edit().putStringSet(KEY, current).apply()
        return current
    }
}

/** Projects whose notifications are silenced. */
object MutedProjectStore {

    private const val KEY = "muted_projects"

    fun all(context: Context): Set<String> =
        SecurePrefs.get(context).getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun toggle(context: Context, projectID: String): Set<String> {
        val prefs = SecurePrefs.get(context)
        val current = prefs.getStringSet(KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (!current.add(projectID)) current.remove(projectID)
        prefs.edit().putStringSet(KEY, current).apply()
        return current
    }
}
