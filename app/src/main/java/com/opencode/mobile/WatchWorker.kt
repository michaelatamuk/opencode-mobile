package com.opencode.mobile

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Safety net for "only on finish": Android may stop the background watcher when
 * the app is closed for a long time, so this checks the active set periodically
 * and posts finished notifications for sessions that disappeared meanwhile.
 */
class WatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = SecurePrefs.get(applicationContext)
        if (!prefs.getBoolean("watch", false)) return@withContext Result.success()
        val server = prefs.getString("server", "").orEmpty()
        if (server.isBlank()) return@withContext Result.success()

        val api = Api(
            server,
            prefs.getString("password", "").orEmpty(),
            prefs.getString("fallback_server", "").orEmpty()
        )
        val sessions = runCatching { api.sessions() }.getOrNull()
            ?: return@withContext Result.retry()

        val running = sessions.filter { it.running }.map { it.id }.toSet()
        val previous = prefs.getStringSet("worker_active", emptySet())?.toSet() ?: emptySet()
        val muted = MutedStore.all(applicationContext)

        (previous - running).forEach { id ->
            val session = sessions.firstOrNull { it.id == id }
            if (session?.parentID?.isNotBlank() == true) return@forEach
            if (id != WatchState.activeSessionID && id !in muted) {
                val title = session?.title
                    ?: applicationContext.getString(R.string.notif_session_fallback)
                postFinished(title, id)
            }
        }
        prefs.edit().putStringSet("worker_active", running).apply()
        Result.success()
    }

    private fun postFinished(title: String, sessionID: String) {
        val context = applicationContext
        Notify.ensureChannels(context)
        val open = PendingIntent.getActivity(
            context,
            sessionID.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra("session_id", sessionID)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, Notify.CHANNEL_SESSIONS)
            .setSmallIcon(R.drawable.ic_stat_opencode)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notif_finished))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        context.getSystemService(android.app.NotificationManager::class.java)
            .notify(sessionID.hashCode(), notification)
    }

    companion object {
        private const val NAME = "watch-fallback"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WatchWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
