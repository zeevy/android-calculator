package com.calculator.core.data.settings

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads/writes user settings through DataStore Preferences.
 *
 * Single source of truth for app-level preferences. Reads return a Flow
 * so anyone observing reacts to live changes; writes are `suspend` and
 * coalesce via DataStore's own write queue.
 */
interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setTheme(theme: UserSettings.ThemeOption)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setHaptics(enabled: Boolean)

    suspend fun setSound(enabled: Boolean)

    suspend fun setPrecision(precision: Int)

    suspend fun setCrashOptIn(enabled: Boolean)

    suspend fun setLastUnitCategory(category: String)

    suspend fun setGstIntraState(intra: Boolean)

    suspend fun setGstRate(rate: String)

    suspend fun setBmiHeightImperial(imperial: Boolean)

    suspend fun setBmiWeightImperial(imperial: Boolean)

    /** Saves the timezone converter's zone pair in one write, so it is never half saved. */
    suspend fun setLastTzPair(
        from: String,
        to: String,
    )
}

@Singleton
class DataStoreSettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : SettingsRepository {
        override val settings: Flow<UserSettings> =
            dataStore.data
                // A read error (not a corrupt file, which DataModule's
                // corruption handler already replaces) falls back to the
                // defaults instead of crashing every screen that observes.
                .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
                .map { prefs ->
                    UserSettings(
                        theme =
                            prefs[Keys.Theme]?.let {
                                runCatching { UserSettings.ThemeOption.valueOf(it) }.getOrNull()
                            } ?: UserSettings.ThemeOption.System,
                        dynamicColor = prefs[Keys.DynamicColor] ?: true,
                        haptics = prefs[Keys.Haptics] ?: true,
                        sound = prefs[Keys.Sound] ?: true,
                        precision = prefs[Keys.Precision] ?: UserSettings.DEFAULT_PRECISION,
                        crashOptIn = prefs[Keys.CrashOptIn] ?: false,
                        lastUnitCategory = prefs[Keys.LastUnitCategory],
                        gstIntraState = prefs[Keys.GstIntraState] ?: true,
                        gstRate = prefs[Keys.GstRate] ?: "18",
                        bmiHeightImperial = prefs[Keys.BmiHeightImperial] ?: false,
                        bmiWeightImperial = prefs[Keys.BmiWeightImperial] ?: false,
                        lastTzFromZone = prefs[Keys.LastTzFromZone],
                        lastTzToZone = prefs[Keys.LastTzToZone],
                    )
                }

        override suspend fun setTheme(theme: UserSettings.ThemeOption) {
            safeEdit { it[Keys.Theme] = theme.name }
        }

        override suspend fun setDynamicColor(enabled: Boolean) {
            safeEdit { it[Keys.DynamicColor] = enabled }
        }

        override suspend fun setHaptics(enabled: Boolean) {
            safeEdit { it[Keys.Haptics] = enabled }
        }

        override suspend fun setSound(enabled: Boolean) {
            safeEdit { it[Keys.Sound] = enabled }
        }

        override suspend fun setPrecision(precision: Int) {
            safeEdit { it[Keys.Precision] = precision.coerceIn(UserSettings.PRECISION_RANGE) }
        }

        override suspend fun setCrashOptIn(enabled: Boolean) {
            safeEdit { it[Keys.CrashOptIn] = enabled }
        }

        override suspend fun setLastUnitCategory(category: String) {
            safeEdit { it[Keys.LastUnitCategory] = category }
        }

        override suspend fun setGstIntraState(intra: Boolean) {
            safeEdit { it[Keys.GstIntraState] = intra }
        }

        override suspend fun setGstRate(rate: String) {
            safeEdit { it[Keys.GstRate] = rate }
        }

        override suspend fun setBmiHeightImperial(imperial: Boolean) {
            safeEdit { it[Keys.BmiHeightImperial] = imperial }
        }

        override suspend fun setBmiWeightImperial(imperial: Boolean) {
            safeEdit { it[Keys.BmiWeightImperial] = imperial }
        }

        override suspend fun setLastTzPair(
            from: String,
            to: String,
        ) {
            safeEdit {
                it[Keys.LastTzFromZone] = from
                it[Keys.LastTzToZone] = to
            }
        }

        // A failed write (disk full, file locked) loses that one change
        // instead of crashing the caller's coroutine.
        private suspend fun safeEdit(change: (MutablePreferences) -> Unit) {
            try {
                dataStore.edit(change)
            } catch (e: IOException) {
                Log.w(TAG, "Could not save a setting", e)
            }
        }

        private companion object {
            const val TAG = "SettingsRepository"
        }

        // Centralised keys so renames stay in one place.
        private object Keys {
            val Theme = stringPreferencesKey("theme")
            val DynamicColor = booleanPreferencesKey("dynamicColor")
            val Haptics = booleanPreferencesKey("haptics")
            val Sound = booleanPreferencesKey("sound")
            val Precision = intPreferencesKey("precision")
            val CrashOptIn = booleanPreferencesKey("crashOptIn")

            // Per-tool last-selection memory.
            val LastUnitCategory = stringPreferencesKey("lastUnitCategory")
            val GstIntraState = booleanPreferencesKey("gstIntraState")
            val GstRate = stringPreferencesKey("gstRate")
            val BmiHeightImperial = booleanPreferencesKey("bmiHeightImperial")
            val BmiWeightImperial = booleanPreferencesKey("bmiWeightImperial")
            val LastTzFromZone = stringPreferencesKey("lastTzFromZone")
            val LastTzToZone = stringPreferencesKey("lastTzToZone")
        }
    }
