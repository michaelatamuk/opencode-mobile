package com.opencode.mobile

import android.app.Notification
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
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/** Which session is on screen right now, so we don't notify about it. */
object WatchState {
    @Volatile
    var activeSessionID: String? = null
}

/**
 * Event-driven watcher: subscribes to the server's SSE stream and reacts to
 * `session.idle`, `permission.asked` and `form.created`, with a slow poll as a
 * safety net if the stream drops.
 */
class SessionWatcherService : Service() {

    companion object {
        const val ACTION_STOP = "com.opencode.mobile.STOP_WATCHING"
        private const val ONGOING_ID = 1001
        private const val FALLBACK_MS = 60_000L
        private const val CHANNEL_ONGOING = Notify.CHANNEL_WATCH
        private const val CHANNEL_SESSIONS = Notify.CHANNEL_SESSIONS
        private const val CHANNEL_PERMISSIONS = Notify.CHANNEL_PERMISSIONS
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var lastActive: Set<String> = emptySet()
    private var titles: MutableMap<String, String> = mutableMapOf()
    private val notifiedPermissions = mutableSetOf<String>()
    private val notifiedForms = mutableSetOf<String>()
    private var primed = false
    private var cachedApi: Api? = null
    private var cachedApiKey: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Notify.ensureChannels(this)
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

        if (job == null) job = scope.launch { loop() }
        return START_STICKY
    }

    private fun api(): Api? {
        val prefs = getSharedPreferences("opencode", MODE_PRIVATE)
        val server = prefs.getString("server", "").orEmpty()
        val password = prefs.getString("password", "").orEmpty()
        if (server.isBlank()) return null

        val key = "$server\u0000$password"
        val existing = cachedApi
        if (existing != null && cachedApiKey == key) return existing

        return Api(server, password).also {
            cachedApi = it
            cachedApiKey = key
        }
    }

    private suspend fun loop() {
        while (scope.isActive) {
            val client = api()
            if (client == null) {
                stopSelf()
                return
            }
            try {
                if (!primed) {
                    pollOnce(client)
                }
                val repo = OpenCodeRepository(client)
                withTimeoutOrNull(FALLBACK_MS) {
                    repo.events().collect { raw -> handleEvent(client, raw) }
                }
                // Stream timed out or ended: re-prime state, then reconnect.
                pollOnce(client)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                delay(5_000)
            }
        }
    }

    private suspend fun handleEvent(client: Api, raw: String) {
        val event = runCatching { JSONObject(raw) }.getOrNull() ?: return
        val type = event.optString("type")
        val data = event.optJSONObject("data")

        when (type) {
            "session.idle" -> {
                val id = data?.optString("sessionID").orEmpty()
                if (id.isNotEmpty() && id != WatchState.activeSessionID) notifyFinished(id)
            }

            "permission.asked" -> syncPermissions(client)

            "form.created" -> {
                val form = data?.optJSONObject("form") ?: return
                val id = form.optString("id")
                val sessionID = form.optString("sessionID")
                val title = form.optString("title")
                    .ifBlank { getString(R.string.notif_form_fallback) }
                if (id.isNotEmpty() && notifiedForms.add(id)) {
                    notifyForm(id, sessionID, title)
                }
            }

            "form.replied", "form.cancelled" -> {
                val id = data?.optJSONObject("form")?.optString("id").orEmpty()
                if (id.isNotEmpty()) {
                    getSystemService(android.app.NotificationManager::class.java).cancel(id.hashCode())
                    notifiedForms.remove(id)
                }
            }
        }
    }

    private suspend fun pollOnce(client: Api) {
        val sessions = client.sessions()
        sessions.forEach { titles[it.id] = it.title }

        val active = sessions.filter { it.running }.map { it.id }.toSet()
        if (primed) {
            (lastActive - active).forEach { id ->
                if (id != WatchState.activeSessionID) notifyFinished(id)
            }
        }
        lastActive = active
        primed = true

        syncPermissions(client)
        updateOngoing(active.size)
    }

    private suspend fun syncPermissions(api: Api) {
        val pending = runCatching { api.permissions() }.getOrDefault(emptyList())
        val ids = pending.map { it.id }.toSet()
        val manager = getSystemService(android.app.NotificationManager::class.java)

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

    private fun sessionIntent(sessionID: String): PendingIntent =
        PendingIntent.getActivity(
            this,
            sessionID.hashCode(),
            Intent(this, MainActivity::class.java)
                .putExtra("session_id", sessionID)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun permissionNotification(request: PendingPermission): Notification {
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
            append(request.action.ifBlank { getString(R.string.permission_notification_body) })
            if (request.detail.isNotBlank()) append('\n').append(request.detail.take(140))
        }

        return NotificationCompat.Builder(this, CHANNEL_PERMISSIONS)
            .setSmallIcon(R.drawable.ic_stat_opencode)
            .setContentTitle(titles[request.sessionID] ?: getString(R.string.permission_notification_title))
            .setContentText(request.action.ifBlank { getString(R.string.permission_notification_body) })
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(sessionIntent(request.sessionID))
            .addAction(action("once", getString(R.string.permission_allow_once)))
            .addAction(action("reject", getString(R.string.permission_reject)))
            .build()
    }

    private fun notifyForm(formID: String, sessionID: String, title: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_PERMISSIONS)
            .setSmallIcon(R.drawable.ic_stat_opencode)
            .setContentTitle(getString(R.string.notif_form_title))
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(sessionIntent(sessionID))
            .build()

        getSystemService(android.app.NotificationManager::class.java)
            .notify(formID.hashCode(), notification)
    }

    private fun notifyFinished(sessionID: String) {
        val title = titles[sessionID] ?: getString(R.string.notif_session_fallback)

        val notification = NotificationCompat.Builder(this, CHANNEL_SESSIONS)
            .setSmallIcon(R.drawable.ic_stat_opencode)
            .setContentTitle(title)
            .setContentText(getString(R.string.notif_finished))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(sessionIntent(sessionID))
            .build()

        getSystemService(android.app.NotificationManager::class.java)
            .notify(sessionID.hashCode(), notification)
    }

    private fun updateOngoing(running: Int) {
        getSystemService(android.app.NotificationManager::class.java)
            .notify(ONGOING_ID, ongoingNotification(running))
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
            running > 0 -> getString(R.string.notif_running, running)
            else -> getString(R.string.notif_waiting)
        }

        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_opencode)
            .setContentTitle(getString(R.string.notif_watching_title))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.action_stop_watching), stop)
            .build()
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        scope.cancel()
        super.onDestroy()
    }
}
