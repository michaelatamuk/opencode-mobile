package com.opencode.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput

/** Notification channels and a manual test, shared by the app and the watcher. */
object Notify {

    const val CHANNEL_WATCH = "watch"
    const val CHANNEL_WATCH_SILENT = "watch_silent"
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
        // Channels are immutable after creation: if an earlier build (or the user)
        // left watch_silent at a higher importance, the placeholder would keep
        // showing in the status bar. Recreate it so "only on finish" stays quiet.
        val staleSilent = manager.getNotificationChannel(CHANNEL_WATCH_SILENT)
        if (staleSilent != null && staleSilent.importance > NotificationManager.IMPORTANCE_MIN) {
            manager.deleteNotificationChannel(CHANNEL_WATCH_SILENT)
        }
        // "Only on finish": the watcher still needs a foreground service to
        // survive in the background, but the placeholder stays invisible in the
        // status bar and never makes a sound.
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_WATCH_SILENT,
                context.getString(R.string.notif_channel_watch_silent),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = context.getString(R.string.notif_channel_watch_silent_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
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

    /** "A run finished" notification with inline reply and quick actions. */
    fun finished(context: Context, sessionID: String, title: String) {
        ensureChannels(context)

        val open = PendingIntent.getActivity(
            context,
            sessionID.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra("session_id", sessionID)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteInput = RemoteInput.Builder(ReplyReceiver.KEY)
            .setLabel(context.getString(R.string.notif_reply_hint))
            .build()
        val replyPending = PendingIntent.getBroadcast(
            context,
            sessionID.hashCode() + 7,
            Intent(context, ReplyReceiver::class.java).putExtra("session_id", sessionID),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val replyAction = NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.notif_reply),
            replyPending
        ).addRemoteInput(remoteInput).build()

        fun quick(action: String, label: Int): NotificationCompat.Action {
            val intent = Intent(context, QuickActionReceiver::class.java)
                .putExtra("session_id", sessionID)
                .putExtra("action", action)
            val pending = PendingIntent.getBroadcast(
                context,
                (sessionID + action).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return NotificationCompat.Action(0, context.getString(label), pending)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SESSIONS)
            .setSmallIcon(R.drawable.ic_stat_opencode)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notif_finished))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(replyAction)
            .addAction(quick(QuickActionReceiver.ACTION_CONTINUE, R.string.notif_quick_continue))
            .addAction(quick(QuickActionReceiver.ACTION_STOP, R.string.notif_quick_stop))
            .addAction(quick(QuickActionReceiver.ACTION_EXPLAIN, R.string.notif_quick_explain))
            .addAction(quick(QuickActionReceiver.ACTION_RETRY, R.string.notif_quick_retry))
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(sessionID.hashCode(), notification)
    }

    private const val TEST_ID = 424242
}
