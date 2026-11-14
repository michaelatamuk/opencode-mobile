package com.opencode.mobile

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking

/**
 * One-tap actions on the finished-session notification, so a turn can be kept
 * moving without opening the app or typing.
 */
class QuickActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sessionID = intent.getStringExtra("session_id") ?: return
        val action = intent.getStringExtra("action") ?: return

        val pending = goAsync()
        Thread {
            try {
                val prefs = SecurePrefs.get(context)
                val api = Api(
                    prefs.getString("server", "").orEmpty(),
                    prefs.getString("password", "").orEmpty(),
                    prefs.getString("fallback_server", "").orEmpty()
                )
                when (action) {
                    ACTION_STOP -> runBlocking { api.interrupt(sessionID) }
                    ACTION_CONTINUE -> runBlocking { api.send(sessionID, PROMPT_CONTINUE) }
                    ACTION_EXPLAIN -> runBlocking { api.send(sessionID, PROMPT_EXPLAIN) }
                    ACTION_RETRY -> runBlocking { api.send(sessionID, PROMPT_RETRY) }
                }
                if (action != ACTION_STOP) {
                    context.getSystemService(NotificationManager::class.java)
                        .cancel(sessionID.hashCode())
                }
            } catch (_: Exception) {
                // Leave the notification in place so the action can be retried.
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_CONTINUE = "continue"
        const val ACTION_STOP = "stop"
        const val ACTION_EXPLAIN = "explain"
        const val ACTION_RETRY = "retry"

        private const val PROMPT_CONTINUE = "Continue."
        private const val PROMPT_EXPLAIN =
            "Explain what you changed and why, in a short summary."
        private const val PROMPT_RETRY =
            "That did not work. Try again with a different approach."
    }
}
