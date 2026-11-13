package com.opencode.mobile

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking

/** Handles Allow / Reject actions from a permission notification. */
class PermissionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionID = intent.getStringExtra("session_id") ?: return
        val requestID = intent.getStringExtra("request_id") ?: return
        val decision = intent.getStringExtra("decision") ?: return
        val pending = goAsync()

        Thread {
            try {
                val prefs = SecurePrefs.get(context)
                val api = Api(
                    prefs.getString("server", "").orEmpty(),
                    prefs.getString("password", "").orEmpty()
                )
                runBlocking { api.reply(sessionID, requestID, decision) }
            } catch (_: Exception) {
                // The request may already be handled; nothing to do.
            } finally {
                context.getSystemService(NotificationManager::class.java)
                    .cancel(requestID.hashCode())
                pending.finish()
            }
        }.start()
    }
}
