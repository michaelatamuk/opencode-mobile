package com.opencode.mobile

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking

/** Sends a reply typed directly in the notification shade. */
class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionID = intent.getStringExtra("session_id") ?: return
        val results = RemoteInput.getResultsFromIntent(intent) ?: return
        val text = results.getCharSequence(KEY)?.toString()?.trim().orEmpty()
        if (text.isBlank()) return

        val pending = goAsync()
        Thread {
            try {
                val prefs = SecurePrefs.get(context)
                val api = Api(
                    prefs.getString("server", "").orEmpty(),
                    prefs.getString("password", "").orEmpty(),
                    prefs.getString("fallback_server", "").orEmpty()
                )
                runBlocking { api.send(sessionID, text) }
                context.getSystemService(NotificationManager::class.java)
                    .cancel(sessionID.hashCode())
            } catch (_: Exception) {
                // Keep the notification so the message can be retried by hand.
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val KEY = "reply_text"
    }
}
