package com.opencode.mobile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick-settings tile showing how many sessions are running. */
class OpenCodeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val prefs = SecurePrefs.get(this)
        val status = prefs.getString("watch_status", "")
            .orEmpty()
            .ifBlank { getString(R.string.widget_waiting) }
        val running = prefs.getInt("watch_running", 0)
        tile.label = getString(R.string.widget_title)
        tile.state = if (running > 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = android.graphics.drawable.Icon.createWithResource(
            this,
            if (running > 0) R.drawable.ic_live_bars else R.drawable.ic_stat_opencode
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = status
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    11,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
