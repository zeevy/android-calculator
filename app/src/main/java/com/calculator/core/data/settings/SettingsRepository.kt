package com.calculator.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    suspend fun setBmiImperial(imperial: Boolean)
}

@Singleton
class DataStoreSettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : SettingsRepository {
        override val settings: Flow<UserSettings> =
            dataStore.data.map { prefs ->
                UserSettings(
                    theme =
                        prefs[Keys.Theme]?.let {
                            runCatching { UserSettings.ThemeOption.valueOf(it) }.getOrNull()
                        } ?: UserSettings.ThemeOption.System,
                    dynamicColor = prefs[Keys.DynamicColor] ?: true,
                    haptics = prefs[Keys.Haptics] ?: true,
                    sound = prefs[Keys.Sound] ?: true,
                    precision = prefs[Keys.Precision] ?: DEFAULT_PRECISION,
                    crashOptIn = prefs[Keys.CrashOptIn] ?: false,
                    lastUnitCategory = prefs[Keys.LastUnitCategory],
                    gstIntraState = prefs[Keys.GstIntraState] ?: true,
                    gstRate = prefs[Keys.GstRate] ?: "18",
                    bmiImperial = prefs[Keys.BmiImperial] ?: false,
                )
            }

        override suspend fun setTheme(theme: UserSettings.ThemeOption) {
            dataStore.edit { it[Keys.Theme] = theme.name }
        }

        override suspend fun setDynamicColor(enabled: Boolean) {
            dataStore.edit { it[Keys.DynamicColor] = enabled }
        }

        override suspend fun setHaptics(enabled: Boolean) {
            dataStore.edit { it[Keys.Haptics] = enabled }
        }

        override suspend fun setSound(enabled: Boolean) {
            dataStore.edit { it[Keys.Sound] = enabled }
        }

        override suspend fun setPrecision(precision: Int) {
            dataStore.edit { it[Keys.Precision] = precision.coerceIn(MIN_PRECISION, MAX_PRECISION) }
        }

        override suspend fun setCrashOptIn(enabled: Boolean) {
            dataStore.edit { it[Keys.CrashOptIn] = enabled }
        }

        override suspend fun setLastUnitCategory(category: String) {
            dataStore.edit { it[Keys.LastUnitCategory] = category }
        }

        override suspend fun setGstIntraState(intra: Boolean) {
            dataStore.edit { it[Keys.GstIntraState] = intra }
        }

        override suspend fun setGstRate(rate: String) {
            dataStore.edit { it[Keys.GstRate] = rate }
        }

        override suspend fun setBmiImperial(imperial: Boolean) {
            dataStore.edit { it[Keys.BmiImperial] = imperial }
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
            val BmiImperial = booleanPreferencesKey("bmiImperial")
        }

        companion object {
            const val DEFAULT_PRECISION = 12
            const val MIN_PRECISION = 6
            const val MAX_PRECISION = 16
        }
    }
