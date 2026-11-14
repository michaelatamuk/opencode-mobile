package com.opencode.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking

/** Widget actions: currently "stop the first running session". */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STOP) return
        val sessionID = intent.getStringExtra("session_id") ?: return
        val pending = goAsync()
        Thread {
            try {
                val prefs = SecurePrefs.get(context)
                val api = Api(
                    prefs.getString("server", "").orEmpty(),
                    prefs.getString("password", "").orEmpty(),
                    prefs.getString("fallback_server", "").orEmpty()
                )
                runBlocking { api.interrupt(sessionID) }
            } catch (_: Exception) {
                // The widget keeps its state; the user can retry from the app.
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_STOP = "com.opencode.mobile.WIDGET_STOP"
    }
}
