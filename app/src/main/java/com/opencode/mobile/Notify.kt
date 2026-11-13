package com.opencode.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

/** Notification channels and a manual test, shared by the app and the watcher. */
object Notify {

    const val CHANNEL_WATCH = "watch"
    const val CHANNEL_SESSIONS = "sessions"
    const val CHANNEL_PERMISSIONS = "permissions"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_WATCH,
                context.getString(R.string.notif_channel_watch),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_watch_desc)
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SESSIONS,
                context.getString(R.string.notif_channel_sessions),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_sessions_desc)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PERMISSIONS,
                context.getString(R.string.notif_channel_permissions),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_permissions_desc)
            }
        )
    }

    fun test(context: Context) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_SESSIONS)
            .setSmallIcon(R.drawable.ic_stat_opencode)
            .setContentTitle(context.getString(R.string.notif_test_title))
            .setContentText(context.getString(R.string.notif_test_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(TEST_ID, notification)
    }

    private const val TEST_ID = 424242
}
