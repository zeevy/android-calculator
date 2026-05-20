package com.calculator.feature.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.calculator.MainActivity
import com.calculator.R
import com.calculator.feature.floating.FloatingShortcutActivity

/**
 * Tracks recently-used tools and republishes the long-press launcher
 * shortcut list. Reads/writes a tiny comma-separated list in
 * SharedPreferences (DataStore would be overkill for at most three
 * IDs) and treats the most-recent entry as the head of the list.
 *
 * The published list is always [Float, recent1, recent2, recent3].
 * Float lives here (as a dynamic shortcut built in code) rather than
 * in `shortcuts.xml` because static intents need a hardcoded
 * `targetPackage` to resolve cleanly - the debug build's `.debug`
 * suffix made every static shortcut surface the "Open with..." app
 * chooser. Dynamic shortcuts use `Intent.setClassName(context, ...)`
 * which fully qualifies the component, so no chooser appears.
 */
internal object RecentToolsRegistry {
    private const val PREFS = "recent_tools"
    private const val KEY_LIST = "list"
    private const val MAX_RECENTS = 3
    private const val FLOAT_SHORTCUT_ID = "floating_calculator"

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
        val current = readRecents(prefs)
        // Move-to-front: deduped, capped at MAX_RECENTS.
        val updated = (listOf(id) + current.filter { it != id }).take(MAX_RECENTS)
        if (updated == current) {
            // The user re-tapped the most-recent tool - nothing to push.
            return
        }
        prefs.edit().putString(KEY_LIST, updated.joinToString(",")).apply()
        syncShortcuts(app, updated)
    }

    /**
     * Republishes the shortcut list from the persisted recents.
     * Call at process start so Float (and any saved recents) are
     * present even before the first navigation of the session.
     */
    fun refresh(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        syncShortcuts(app, readRecents(prefs))
    }

    private fun readRecents(prefs: android.content.SharedPreferences): List<String> =
        prefs.getString(KEY_LIST, "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    private fun syncShortcuts(context: Context, recentIds: List<String>) {
        val shortcuts = buildList {
            add(buildFloatShortcut(context))
            recentIds.mapNotNullTo(this) { buildRecentShortcut(context, it) }
        }
        // setDynamicShortcuts replaces the whole set, which is what we
        // want - old entries that are no longer in `recents` should
        // disappear; Float gets re-added every time.
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }

    private fun buildFloatShortcut(context: Context): ShortcutInfoCompat {
        val intent = Intent(Intent.ACTION_VIEW)
            .setClassName(context, FloatingShortcutActivity::class.java.name)
        return ShortcutInfoCompat.Builder(context, FLOAT_SHORTCUT_ID)
            .setShortLabel(context.getString(R.string.shortcut_floating_short))
            .setLongLabel(context.getString(R.string.shortcut_floating_long))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_round))
            .setIntent(intent)
            .build()
    }

    private fun buildRecentShortcut(context: Context, toolId: String): ShortcutInfoCompat? {
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
