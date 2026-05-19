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
        // Explicit component intent rather than action+category because
        // the tile lives inside the system's shade process, and that
        // process doesn't share our class loader - implicit intents
        // would only work if MainActivity advertised the right
        // category in the manifest, which is unnecessary here.
        // FLAG_ACTIVITY_NEW_TASK is required because we're launching
        // from outside an activity; CLEAR_TOP brings any existing
        // calculator task forward instead of stacking a duplicate.
        val launchIntent =
            Intent().apply {
                component = ComponentName(applicationContext, "com.calculator.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+ requires a PendingIntent so the platform can
            // attribute the launch to our app rather than to the shade.
            // FLAG_IMMUTABLE is mandatory on modern Android for any
            // PendingIntent we don't intend the receiver to mutate.
            val pi =
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            startActivityAndCollapse(pi)
        } else {
            // Pre-34 path: the Intent overload is deprecated but still
            // the only option available, and the platform doesn't yet
            // require a PendingIntent here. Suppression is local so a
            // future minSdk bump above 34 will surface it again.
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(launchIntent)
        }
    }
}
