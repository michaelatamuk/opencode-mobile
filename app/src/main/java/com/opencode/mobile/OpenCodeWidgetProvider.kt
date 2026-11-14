package com.opencode.mobile

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, build(context, status))
        }
    }

    companion object {
        fun updateAll(context: Context, status: String) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, OpenCodeWidgetProvider::class.java)
                )
                if (ids.isEmpty()) return
                val views = build(context, status)
                ids.forEach { manager.updateAppWidget(it, views) }
            }
        }

        private fun build(context: Context, status: String): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_opencode).apply {
                setTextViewText(R.id.widget_status, status)
                setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context,
                        10,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
    }
}
