package com.opencode.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Which session is on screen right now, so we don't notify about it. */
object WatchState {
    @Volatile
    var activeSessionID: String? = null
}

/**
 * Keeps one background poll against the OpenCode server and posts a notification
 * when a session that was running becomes idle.
 */
class SessionWatcherService : Service() {

    companion object {
        const val ACTION_STOP = "com.opencode.mobile.STOP_WATCHING"
        private const val ONGOING_ID = 1001
        private const val CHANNEL_ONGOING = "watch"
        private const val CHANNEL_SESSIONS = "sessions"
        private const val CHANNEL_PERMISSIONS = "permissions"
        private const val POLL_MS = 10_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var lastActive: Set<String> = emptySet()
    private var titles: MutableMap<String, String> = mutableMapOf()
    private val notifiedPermissions = mutableSetOf<String>()
    private var primed = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)

        val ongoing = NotificationChannel(
            CHANNEL_ONGOING,
            "Watcher",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the app connected while it watches your sessions"
            setShowBadge(false)
        }
        val sessions = NotificationChannel(
            CHANNEL_SESSIONS,
            "Session finished",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Tells you when a session stops working" }

        val permissions = NotificationChannel(
            CHANNEL_PERMISSIONS,
            "Permission requests",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "A session is waiting for your approval" }

        manager.createNotificationChannel(ongoing)
        manager.createNotificationChannel(sessions)
        manager.createNotificationChannel(permissions)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            getSharedPreferences("opencode", MODE_PRIVATE).edit().putBoolean("watch", false).apply()
            stopSelf()
            return START_NOT_STICKY
        }

        val initial = ongoingNotification(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(ONGOING_ID, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(ONGOING_ID, initial)
        }

        if (job == null) job = scope.launch { watch() }
        return START_STICKY
    }

    private suspend fun watch() {
        while (scope.isActive) {
            try {
                val prefs = getSharedPreferences("opencode", MODE_PRIVATE)
                val server = prefs.getString("server", "").orEmpty()
                val password = prefs.getString("password", "").orEmpty()
                if (server.isBlank()) {
                    stopSelf()
                    return
                }

                val api = Api(server, password)
                val sessions = api.sessions()
                val active = api.activeSessions()
                sessions.forEach { titles[it.id] = it.title }

                if (primed) {
                    (lastActive - active).forEach { id ->
                        if (id != WatchState.activeSessionID) notifyFinished(id)
                    }
                }
                lastActive = active
                primed = true

                syncPermissions(api)
                updateOngoing(active.size)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Server unreachable, tunnel down, etc. Keep watching.
            }
            delay(POLL_MS)
        }
    }

    private suspend fun syncPermissions(api: Api) {
        val pending = runCatching { api.permissions() }.getOrDefault(emptyList())
        val ids = pending.map { it.id }.toSet()
        val manager = getSystemService(NotificationManager::class.java)

        // Drop notifications for requests that were answered somewhere else.
        (notifiedPermissions - ids).forEach { id ->
            manager.cancel(id.hashCode())
            notifiedPermissions.remove(id)
        }

        pending.forEach { request ->
            if (notifiedPermissions.add(request.id)) {
                manager.notify(request.id.hashCode(), permissionNotification(request))
            }
        }
    }

    private fun permissionNotification(request: PendingPermission): Notification {
        val open = PendingIntent.getActivity(
            this,
            request.id.hashCode(),
            Intent(this, MainActivity::class.java)
                .putExtra("session_id", request.sessionID)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun action(decision: String, label: String): NotificationCompat.Action {
            val intent = Intent(this, PermissionActionReceiver::class.java)
                .setAction("com.opencode.mobile.PERMISSION_ACTION")
                .putExtra("session_id", request.sessionID)
                .putExtra("request_id", request.id)
                .putExtra("decision", decision)
            val pending = PendingIntent.getBroadcast(
                this,
                (request.id + decision).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return NotificationCompat.Action(0, label, pending)
        }

        val body = buildString {
            append(request.action.ifBlank { "A tool needs approval" })
            if (request.detail.isNotBlank()) append('\n').append(request.detail.take(140))
        }

        return NotificationCompat.Builder(this, CHANNEL_PERMISSIONS)
            .setSmallIcon(R.drawable.ic_stat_opencode)
            .setContentTitle(titles[request.sessionID] ?: "Permission needed")
            .setContentText(request.action.ifBlank { "A tool needs approval" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(action("once", "Allow"))
            .addAction(action("reject", "Reject"))
            .build()
    }

    private fun notifyFinished(sessionID: String) {
        val title = titles[sessionID] ?: "Session"
        val open = Intent(this, MainActivity::class.java)
            .putExtra("session_id", sessionID)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            this,
            sessionID.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_SESSIONS)
            .setSmallIcon(R.drawable.ic_stat_opencode)
            .setContentTitle(title)
            .setContentText("Finished — tap to open")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(sessionID.hashCode(), notification)
    }

    private fun updateOngoing(running: Int) {
        getSystemService(NotificationManager::class.java).notify(ONGOING_ID, ongoingNotification(running))
    }

    private fun ongoingNotification(running: Int): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, SessionWatcherService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = when {
            running > 0 -> "$running session${if (running == 1) "" else "s"} running"
            else -> "Waiting for a session to run"
        }

        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_opencode)
            .setContentTitle("OpenCode")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, "Stop watching", stop)
            .build()
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        scope.cancel()
        super.onDestroy()
    }
}
