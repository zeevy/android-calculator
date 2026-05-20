package com.calculator.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculator.core.data.settings.SettingsRepository
import com.calculator.core.data.settings.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Settings screen.
 *
 * The repository is the single source of truth for persisted settings;
 * the ViewModel only translates between repository Flow and the UI's
 * StateFlow contract. Writes are fire-and-forget into the repository -
 * the Flow re-emits the new value, which Compose collects.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) : ViewModel() {
        val settings: StateFlow<UserSettings> =
            repository.settings.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = UserSettings(),
            )

        fun setTheme(theme: UserSettings.ThemeOption) {
            viewModelScope.launch { repository.setTheme(theme) }
        }

        fun setDynamicColor(enabled: Boolean) {
            viewModelScope.launch { repository.setDynamicColor(enabled) }
        }

        fun setHaptics(enabled: Boolean) {
            viewModelScope.launch { repository.setHaptics(enabled) }
        }

        fun setSound(enabled: Boolean) {
            viewModelScope.launch { repository.setSound(enabled) }
        }

        fun setPrecision(precision: Int) {
            viewModelScope.launch { repository.setPrecision(precision) }
        }

        fun setCrashOptIn(enabled: Boolean) {
            viewModelScope.launch { repository.setCrashOptIn(enabled) }
        }

        fun setGstIntraState(intra: Boolean) {
            viewModelScope.launch { repository.setGstIntraState(intra) }
        }

        fun setGstRate(rate: String) {
            viewModelScope.launch { repository.setGstRate(rate) }
        }

        fun setBmiHeightImperial(imperial: Boolean) {
            viewModelScope.launch { repository.setBmiHeightImperial(imperial) }
        }

        fun setBmiWeightImperial(imperial: Boolean) {
            viewModelScope.launch { repository.setBmiWeightImperial(imperial) }
        }
    }
