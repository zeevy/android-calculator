package com.calculator.navigation

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.calculator.MainActivity
import com.calculator.R
import com.calculator.feature.floating.FloatingShortcutActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.Locale

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
    private const val TAG = "RecentTools"

    // Preferences and ShortcutManager do disk and binder work, so it runs
    // off the main thread, one call at a time so two quick visits cannot
    // overwrite each other's list. Lives as long as the process, like the
    // shortcuts it publishes.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    /**
     * Records that the user opened the tool with this [Tool.id] and
     * rebuilds the dynamic shortcut list, in the background.
     */
    fun record(
        context: Context,
        id: String,
    ) {
        if (Tools.any { it.id == id && it.keepOutOfRecents }) return
        val app = context.applicationContext
        scope.launch { recordNow(app, id) }
    }

    private fun recordNow(
        app: Context,
        id: String,
    ) {
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
        scope.launch { refreshNow(app) }
    }

    private fun refreshNow(app: Context) {
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // A private tool saved by an older version is dropped from the file
        // too, so it does not stay in backups. Tools that are only hidden
        // here (GST outside India) stay saved for when they come back.
        val saved = prefs
            .getString(KEY_LIST, "")
            .orEmpty()
            .split(",")
            .filter { it.isNotBlank() }
        val withoutPrivate = saved.filterNot { id -> Tools.any { it.id == id && it.keepOutOfRecents } }
        if (withoutPrivate != saved) prefs.edit().putString(KEY_LIST, withoutPrivate.joinToString(",")).apply()
        syncShortcuts(app, readRecents(prefs))
    }

    // Ids of tools not offered on this device (GST outside India) and of
    // private tools (saved before they were marked private) are skipped,
    // so they do not take one of the MAX_RECENTS slots.
    private fun readRecents(prefs: android.content.SharedPreferences): List<String> =
        prefs
            .getString(KEY_LIST, "")
            ?.split(",")
            ?.filter { availableToolById(it, Locale.getDefault(), ZoneId.systemDefault())?.keepOutOfRecents == false }
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
            .onFailure { Log.w(TAG, "Could not publish launcher shortcuts", it) }
    }

    private fun buildFloatShortcut(context: Context): ShortcutInfoCompat {
        val intent = Intent(Intent.ACTION_VIEW)
            .setClassName(context, FloatingShortcutActivity::class.java.name)
        return ShortcutInfoCompat
            .Builder(context, FLOAT_SHORTCUT_ID)
            .setShortLabel(context.getString(R.string.shortcut_floating_short))
            .setLongLabel(context.getString(R.string.shortcut_floating_long))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_round))
            .setIntent(intent)
            .build()
    }

    private fun buildRecentShortcut(context: Context, toolId: String): ShortcutInfoCompat? {
        // A tool not offered on this device (GST outside India) gets no shortcut.
        val entry = availableToolById(toolId, Locale.getDefault(), ZoneId.systemDefault()) ?: return null
        val intent = Intent(Intent.ACTION_VIEW)
            .setClassName(context, MainActivity::class.java.name)
            .putExtra(MainActivity.SHORTCUT_DESTINATION, toolId)
        return ShortcutInfoCompat
            .Builder(context, "recent_$toolId")
            .setShortLabel(context.getString(entry.label))
            .setLongLabel(context.getString(entry.longLabel))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_round))
            .setIntent(intent)
            .build()
    }
}
