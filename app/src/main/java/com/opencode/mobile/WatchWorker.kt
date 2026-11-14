package com.opencode.mobile

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
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
            val onScreen = WatchState.appForeground && id == WatchState.activeSessionID
            if (!onScreen && id !in muted) {
                val title = session?.title
                    ?: applicationContext.getString(R.string.notif_session_fallback)
                Notify.finished(applicationContext, id, title)
            }
        }
        prefs.edit().putStringSet("worker_active", running).apply()
        Result.success()
    }

    companion object {
        private const val NAME = "watch-fallback"
        private const val NOW = "watch-now"

        /** Expedited check when the app opens, to catch a run missed in the background. */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WatchWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                NOW,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

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
