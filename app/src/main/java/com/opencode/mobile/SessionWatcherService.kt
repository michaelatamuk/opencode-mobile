package com.opencode.mobile

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.TileService
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
import java.util.concurrent.ConcurrentHashMap

/** Which session is on screen right now, so we don't notify about it. */
object WatchState {
    @Volatile
    var activeSessionID: String? = null

    /** True while any activity is resumed, so "seen" suppression only applies then. */
    @Volatile
    var appForeground: Boolean = false
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
    private var watchdogJob: Job? = null
    private var lastActive: Set<String> = emptySet()
    private var titles: MutableMap<String, String> = mutableMapOf()
    private val sessionProjects = mutableMapOf<String, String>()
    private val sessionParents = mutableMapOf<String, String>()
    private val notifiedPermissions = mutableSetOf<String>()
    private val notifiedForms = mutableSetOf<String>()
    private var primed = false
    private var cachedApi: Api? = null
    private var cachedApiKey: String? = null

    /** A session currently working, with the tool it is running. */
    private class LiveRun(
        @Volatile var startedAt: Long,
        @Volatile var tool: String? = null
    )

    private val runs = ConcurrentHashMap<String, LiveRun>()
    private val lastFinished = ConcurrentHashMap<String, Long>()

    @Volatile
    private var status: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Notify.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            SecurePrefs.get(this).edit().putBoolean("watch", false).apply()
            stopSelf()
            return START_NOT_STICKY
        }

        val initial = ongoingNotification()
        // Both modes run as a foreground service: a plain background service is
        // stopped by Android shortly after the app leaves the screen, which is
        // why "only on finish" used to miss every completion. Finish-only uses a
        // silent, status-bar-less notification instead of the live progress one.
        startForegroundCompat(initial)

        if (job == null) job = scope.launch { loop() }
        if (watchdogJob == null) watchdogJob = scope.launch { watchdogLoop() }
        return START_STICKY
    }

    /** Stops runs that exceed the user's time limit, checked while streaming. */
    private suspend fun watchdogLoop() {
        while (scope.isActive) {
            delay(30_000)
            val minutes = SecurePrefs.get(this).getInt("watchdog_minutes", 0)
            if (minutes <= 0) continue
            val client = api() ?: continue
            val cutoff = System.currentTimeMillis() - minutes * 60_000L
            runs.filterValues { it.startedAt < cutoff }.keys.forEach { id ->
                runCatching { client.interrupt(id) }
                runs.remove(id)
            }
            updateOngoing()
        }
    }

    private fun api(): Api? {
        val prefs = SecurePrefs.get(this)
        val server = prefs.getString("server", "").orEmpty()
        val password = prefs.getString("password", "").orEmpty()
        val fallback = prefs.getString("fallback_server", "").orEmpty()
        if (server.isBlank()) return null

        val key = "$server\u0000$password\u0000$fallback"
        val existing = cachedApi
        if (existing != null && cachedApiKey == key) return existing

        return Api(server, password, fallback).also {
            cachedApi = it
            cachedApiKey = key
        }
    }

    private suspend fun loop() {
        while (scope.isActive) {
            val client = api()
            if (client == null) {
                status = getString(R.string.notif_no_server)
                updateOngoing()
                delay(10_000)
                continue
            }
            try {
                if (!primed) {
                    pollOnce(client)
                }
                status = null
                val repo = OpenCodeRepository(client)
                withTimeoutOrNull(FALLBACK_MS) {
                    repo.events().collect { raw -> handleEvent(client, raw) }
                }
                // Stream timed out or ended: re-prime state, then reconnect.
                pollOnce(client)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                status = getString(R.string.notif_offline)
                updateOngoing()
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
                if (id.isNotEmpty()) {
                    runs.remove(id)
                    updateOngoing()
                    if (shouldNotify(id)) notifyFinished(id)
                }
            }

            "session.status" -> {
                val id = data?.optString("sessionID").orEmpty()
                if (id.isNotEmpty()) {
                    when (data?.optJSONObject("status")?.optString("type")) {
                        "busy", "retry" -> markRunning(id)
                        "idle" -> {
                            // Some server builds report completion as a status
                            // change instead of a session.idle event.
                            val wasRunning = runs.remove(id) != null
                            updateOngoing()
                            if (wasRunning && shouldNotify(id)) notifyFinished(id)
                        }
                    }
                }
            }

            "session.execution.started" -> {
                val id = data?.optString("sessionID").orEmpty()
                if (id.isNotEmpty()) markRunning(id)
            }

            "session.execution.failed", "session.execution.interrupted" -> {
                val id = data?.optString("sessionID").orEmpty()
                if (id.isNotEmpty()) {
                    runs.remove(id)
                    updateOngoing()
                }
            }

            "session.tool.input.started" -> {
                val id = data?.optString("sessionID").orEmpty()
                val name = data?.optString("name").orEmpty()
                if (id.isNotEmpty()) markRunning(id, name.ifBlank { null })
            }

            "session.tool.called" -> {
                val id = data?.optString("sessionID").orEmpty()
                if (id.isNotEmpty()) markRunning(id)
            }

            "session.tool.success", "session.tool.failed" -> {
                val id = data?.optString("sessionID").orEmpty()
                runs[id]?.let { if (it.tool != null) { it.tool = null; updateOngoing() } }
            }

            "permission.asked" -> syncPermissions(client)

            "form.created" -> {
                val form = data?.optJSONObject("form") ?: return
                val id = form.optString("id")
                val sessionID = form.optString("sessionID")
                val title = form.optString("title")
                    .ifBlank { getString(R.string.notif_form_fallback) }
                if (id.isNotEmpty() && notifiedForms.add(id) && !muted(sessionID)) {
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

    /** Records a session as working and refreshes the ongoing notification. */
    private fun markRunning(id: String, tool: String? = null) {
        val existing = runs[id]
        if (existing == null) {
            runs[id] = LiveRun(System.currentTimeMillis(), tool)
        } else {
            if (tool == null) return
            if (existing.tool == tool) return
            existing.tool = tool
        }
        updateOngoing()
    }

    private suspend fun pollOnce(client: Api) {
        val sessions = client.sessions()
        UsageStore.record(this, sessions)
        sessions.forEach { titles[it.id] = it.title }
        sessions.forEach { sessionProjects[it.id] = it.projectID }
        sessions.forEach { sessionParents[it.id] = it.parentID }

        val active = sessions.filter { it.running }.map { it.id }.toSet()
        if (primed) {
            (lastActive - active).forEach { id ->
                if (shouldNotify(id)) notifyFinished(id)
            }
        }
        lastActive = active
        primed = true

        // Keep the live map in step with the server, so a run that started
        // before the watcher did still shows up in the ongoing notification.
        runs.keys.retainAll(active)
        active.forEach { id -> if (!runs.containsKey(id)) runs[id] = LiveRun(System.currentTimeMillis()) }

        syncPermissions(client)
        flushPending(client)
        UsageStore.record(this, sessions)
        checkBudget()
        updateOngoing()
    }

    /** Delivers prompts that were queued while the server was unreachable. */
    private suspend fun flushPending(client: Api) {
        PendingStore.all(this).forEach { prompt ->
            try {
                val files = prompt.attachments.mapNotNull { att ->
                    runCatching {
                        Attachment(
                            att.name,
                            att.mime,
                            android.util.Base64.encodeToString(
                                java.io.File(att.path).readBytes(),
                                android.util.Base64.NO_WRAP
                            )
                        )
                    }.getOrNull()
                }
                client.send(
                    sessionID = prompt.sessionID,
                    text = prompt.text,
                    files = files,
                    skillIDs = prompt.skillIDs
                )
                PendingStore.remove(this, prompt.id)
                prompt.attachments.forEach { runCatching { java.io.File(it.path).delete() } }
            } catch (e: ApiException) {
                if (e.status in 400..499) PendingStore.remove(this, prompt.id)
            } catch (_: Exception) {
                // Keep it queued for the next attempt.
            }
        }
    }

    /** Warns once per day/month when spend reaches the configured caps. */
    private fun checkBudget() {
        val prefs = SecurePrefs.get(this)
        val day = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        val open = PendingIntent.getActivity(
            this,
            3,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun notify(id: Int, title: String, body: String) {
            val notification = NotificationCompat.Builder(this, CHANNEL_SESSIONS)
                .setSmallIcon(R.drawable.ic_stat_opencode)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build()
            getSystemService(android.app.NotificationManager::class.java).notify(id, notification)
        }

        val daily = prefs.getString("budget_usd", "")?.toDoubleOrNull()
        if (daily != null && daily > 0.0) {
            val spent = UsageStore.today(this)
            if (spent >= daily && prefs.getString("budget_alerted_day", "") != day) {
                prefs.edit().putString("budget_alerted_day", day).apply()
                notify(
                    4_100_001,
                    getString(R.string.notif_budget_title),
                    getString(R.string.notif_budget_body, spent, daily)
                )
            }
        }

        val monthly = prefs.getString("budget_month_usd", "")?.toDoubleOrNull()
        if (monthly != null && monthly > 0.0) {
            val spentMonth = UsageStore.monthToDate(this)
            val monthKey = day.take(7)
            if (spentMonth >= monthly &&
                prefs.getString("budget_month_alerted", "") != monthKey
            ) {
                prefs.edit().putString("budget_month_alerted", monthKey).apply()
                notify(
                    4_100_002,
                    getString(R.string.notif_budget_month_title),
                    getString(R.string.notif_budget_month_body, spentMonth, monthly)
                )
            }
        }
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
            if (notifiedPermissions.add(request.id) && !muted(request.sessionID)) {
                manager.notify(request.id.hashCode(), permissionNotification(request))
            }
        }
    }

    private fun muted(sessionID: String): Boolean {
        if (sessionID in MutedStore.all(this)) return true
        val project = sessionProjects[sessionID] ?: return false
        return project in MutedProjectStore.all(this)
    }

    /** Subagent runs are contained in their parent; the parent notifies instead. */
    private fun isChild(sessionID: String): Boolean =
        !sessionParents[sessionID].isNullOrBlank()

    /**
     * A run only stays quiet when the session is genuinely on screen. With the
     * app in the background the chat still counted as "active", which used to
     * swallow every finish notification.
     */
    private fun shouldNotify(sessionID: String): Boolean =
        !muted(sessionID) &&
            !isChild(sessionID) &&
            !(WatchState.appForeground && sessionID == WatchState.activeSessionID)

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
        // `session.status: idle` and `session.idle` can both arrive for the same
        // turn; keep the second one from alerting again.
        val now = System.currentTimeMillis()
        val previous = lastFinished[sessionID]
        if (previous != null && now - previous < 10_000L) return
        lastFinished[sessionID] = now
        Notify.finished(this, sessionID, titleFor(sessionID))
    }

    private fun persistentNotification(): Boolean =
        SecurePrefs.get(this).getBoolean("watch_persistent", false)

    private fun startForegroundCompat(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(ONGOING_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(ONGOING_ID, notification)
            }
        } catch (_: Exception) {
            // The OS can refuse a foreground promotion from the background; the
            // watcher keeps running as a normal service instead.
        }
    }

    private fun updateOngoing() {
        val label = statusLabel()
        SecurePrefs.get(this).edit()
            .putString("watch_status", label)
            .putInt("watch_running", runs.size)
            .apply()
        OpenCodeWidgetProvider.updateAll(
            this,
            label,
            runs.keys.take(3).joinToString("\n") { titleFor(it) },
            runs.keys.firstOrNull()
        )
        runCatching {
            TileService.requestListeningState(
                this,
                ComponentName(this, OpenCodeTileService::class.java)
            )
        }

        // Both modes keep a foreground service so the event stream survives in
        // the background; finish-only simply uses the silent channel.
        startForegroundCompat(ongoingNotification())
    }

    private fun statusLabel(): String = when {
        status != null -> status!!
        runs.isEmpty() -> getString(R.string.notif_waiting)
        runs.size == 1 -> titleFor(runs.keys.first())
        else -> getString(R.string.notif_running, runs.size)
    }

    private fun titleFor(sessionID: String): String =
        titles[sessionID] ?: getString(R.string.notif_session_fallback)

    private fun ongoingNotification(): Notification {
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

        val single = runs.entries.firstOrNull()
        val silent = !persistentNotification()
        // Never mirror live run state into the finish-only placeholder: it must
        // not read (or update) like a "running task" notification.
        val text = if (silent) getString(R.string.notif_watch_silent_text) else statusLabel()

        val builder = NotificationCompat.Builder(
            this,
            if (silent) Notify.CHANNEL_WATCH_SILENT else CHANNEL_ONGOING
        )
            .setSmallIcon(R.drawable.ic_stat_opencode)
            .setContentTitle(getString(R.string.notif_watching_title))
            .setContentText(text)
            .setPriority(if (silent) NotificationCompat.PRIORITY_MIN else NotificationCompat.PRIORITY_LOW)
            .setSilent(silent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.action_stop_watching), stop)

        if (!silent && status == null && runs.size == 1 && single != null) {
            // The system clock ticks the elapsed time for free; tool changes
            // arrive as events, so no polling timer is needed here.
            val run = single.value
            builder.setWhen(run.startedAt).setShowWhen(true).setUsesChronometer(true)
            val tool = run.tool
            builder.setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    if (tool != null) "${titleFor(single.key)}\n$tool" else titleFor(single.key)
                )
            )
        } else if (status == null && runs.size > 1) {
            val lines = runs.entries.take(5).joinToString("\n") { entry ->
                val tool = entry.value.tool
                if (tool != null) "${titleFor(entry.key)} · $tool" else titleFor(entry.key)
            }
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(lines))
        }

        return builder.build()
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        scope.cancel()
        super.onDestroy()
    }
}
