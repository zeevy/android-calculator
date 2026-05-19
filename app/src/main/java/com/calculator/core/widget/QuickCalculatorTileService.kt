package com.calculator.core.widget

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile: tap the tile in the notification shade to open
 * the calculator directly.
 *
 * Behaviour:
 *  - Tile state is always [Tile.STATE_INACTIVE] when the tile is shown
 *    (it's a launcher, not a toggle).
 *  - On click, builds an explicit intent for `MainActivity` and uses
 *    `startActivityAndCollapse` on Android 14+ (which requires a
 *    PendingIntent) or the legacy Intent overload on older versions.
 */
class QuickCalculatorTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = applicationContext.getString(com.calculator.R.string.app_name)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val launchIntent =
            Intent().apply {
                component = ComponentName(applicationContext, "com.calculator.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi =
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(launchIntent)
        }
    }
}
