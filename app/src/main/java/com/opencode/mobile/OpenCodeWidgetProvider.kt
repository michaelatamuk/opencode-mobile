package com.opencode.mobile

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

/** Home-screen widget: what the watcher is doing right now, tap to open. */
class OpenCodeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val status = SecurePrefs.get(context)
            .getString("watch_status", "")
            .orEmpty()
            .ifBlank { context.getString(R.string.widget_waiting) }
        val lines = SecurePrefs.get(context).getString("watch_lines", "").orEmpty()
        val stopID = SecurePrefs.get(context).getString("watch_stop_id", "").orEmpty()
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, build(context, status, lines, stopID))
        }
    }

    companion object {
        fun refresh(context: Context) {
            val prefs = SecurePrefs.get(context)
            val status = prefs.getString("watch_status", "")
                .orEmpty()
                .ifBlank { context.getString(R.string.widget_waiting) }
            updateAll(
                context,
                status,
                prefs.getString("watch_lines", "").orEmpty(),
                prefs.getString("watch_stop_id", "").orEmpty().ifBlank { null }
            )
        }

        fun updateAll(context: Context, status: String, lines: String, stopSessionID: String?) {
            SecurePrefs.get(context).edit()
                .putString("watch_lines", lines)
                .putString("watch_stop_id", stopSessionID.orEmpty())
                .apply()
            runCatching {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, OpenCodeWidgetProvider::class.java)
                )
                if (ids.isEmpty()) return
                val views = build(context, status, lines, stopSessionID)
                ids.forEach { manager.updateAppWidget(it, views) }
            }
        }

        private fun build(
            context: Context,
            status: String,
            lines: String,
            stopSessionID: String?
        ): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_opencode).apply {
                val prefs = SecurePrefs.get(context)
                val showTitles = prefs.getBoolean("widget_show_titles", true)
                val showStop = prefs.getBoolean("widget_show_stop", true)
                setTextViewText(R.id.widget_status, status)
                if (lines.isBlank() || !showTitles) {
                    setViewVisibility(R.id.widget_lines, View.GONE)
                } else {
                    setViewVisibility(R.id.widget_lines, View.VISIBLE)
                    setTextViewText(R.id.widget_lines, lines)
                }
                setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context,
                        10,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                if (stopSessionID.isNullOrBlank() || !showStop) {
                    setViewVisibility(R.id.widget_stop, View.GONE)
                } else {
                    setViewVisibility(R.id.widget_stop, View.VISIBLE)
                    val stop = Intent(context, WidgetActionReceiver::class.java)
                        .setAction(WidgetActionReceiver.ACTION_STOP)
                        .putExtra("session_id", stopSessionID)
                    setOnClickPendingIntent(
                        R.id.widget_stop,
                        PendingIntent.getBroadcast(
                            context,
                            stopSessionID.hashCode() + 3,
                            stop,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            }
    }
}
