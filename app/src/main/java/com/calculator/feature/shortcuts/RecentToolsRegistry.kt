package com.calculator.feature.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.calculator.MainActivity
import com.calculator.R

/**
 * Tracks recently-used tools and republishes them as dynamic launcher
 * shortcuts. Reads/writes a tiny comma-separated list in
 * SharedPreferences (DataStore would be overkill for at most three
 * IDs) and treats the most-recent entry as the head of the list.
 *
 * Static "Floating calculator" stays first in the long-press menu via
 * the static shortcuts XML; this registry only pushes the rest.
 */
internal object RecentToolsRegistry {
    private const val PREFS = "recent_tools"
    private const val KEY_LIST = "list"
    private const val MAX_RECENTS = 3

    /**
     * Records that the user navigated to [route] and rebuilds the
     * dynamic shortcut list. Routes not in [ToolShortcutCatalog]
     * (BasicCalculatorRoute, the trampoline activity, etc.) are
     * silently ignored.
     */
    fun record(context: Context, route: Any) {
        val id = ToolShortcutCatalog.idOf(route) ?: return
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_LIST, "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        // Move-to-front: deduped, capped at MAX_RECENTS.
        val updated = (listOf(id) + current.filter { it != id }).take(MAX_RECENTS)
        if (updated == current) {
            // The user re-tapped the most-recent tool - nothing to push.
            return
        }
        prefs.edit().putString(KEY_LIST, updated.joinToString(",")).apply()
        syncShortcuts(app, updated)
    }

    private fun syncShortcuts(context: Context, recentIds: List<String>) {
        val shortcuts = recentIds.mapNotNull { buildShortcut(context, it) }
        // setDynamicShortcuts replaces the whole set, which is what we
        // want - old entries that are no longer in `recents` should
        // disappear.
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }

    private fun buildShortcut(context: Context, toolId: String): ShortcutInfoCompat? {
        val entry = ToolShortcutCatalog.byId(toolId) ?: return null
        val intent = Intent(Intent.ACTION_VIEW)
            .setClassName(context, MainActivity::class.java.name)
            .putExtra(MainActivity.SHORTCUT_DESTINATION, toolId)
        return ShortcutInfoCompat.Builder(context, "recent_$toolId")
            .setShortLabel(context.getString(entry.shortLabelRes))
            .setLongLabel(context.getString(entry.longLabelRes))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_round))
            .setIntent(intent)
            .build()
    }
}
