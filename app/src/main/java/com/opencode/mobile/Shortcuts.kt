package com.opencode.mobile

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Launcher shortcuts for the three most recent sessions, alongside the static
 * "New session" entry. Kept in sync from the cached session list so it works
 * even before the first network call of a launch.
 */
object Shortcuts {

    private const val MAX_SESSIONS = 3

    fun sync(context: Context) {
        runCatching {
            val recent = SessionCache(context).read()
                .filter { it.id.isNotBlank() }
                .take(MAX_SESSIONS)
            if (recent.isEmpty()) return@runCatching

            val icon = IconCompat.createWithResource(context, R.mipmap.ic_launcher)
            val shortcuts = recent.map { session ->
                ShortcutInfoCompat.Builder(context, "session_${session.id}")
                    .setShortLabel(session.title.take(30).ifBlank { "Session" })
                    .setLongLabel(session.title.take(60))
                    .setIcon(icon)
                    .setIntent(
                        Intent(context, MainActivity::class.java)
                            .setAction(Intent.ACTION_VIEW)
                            .putExtra("session_id", session.id)
                    )
                    .build()
            }
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }
    }
}
