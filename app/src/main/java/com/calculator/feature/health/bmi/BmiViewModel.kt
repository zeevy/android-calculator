package com.calculator.feature.health.bmi

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

/** Remembers the BMI screen's last height and weight units. */
@HiltViewModel
class BmiViewModel
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) : ViewModel() {
        /** Saved settings; the screen reads `bmiHeightImperial` and `bmiWeightImperial`. */
        val settings: StateFlow<UserSettings> =
            repository.settings.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = UserSettings(),
            )

        /** Saves whether height is entered in feet and inches. */
        fun setHeightImperial(imperial: Boolean) {
            viewModelScope.launch { repository.setBmiHeightImperial(imperial) }
        }

        /** Saves whether weight is entered in pounds. */
        fun setWeightImperial(imperial: Boolean) {
            viewModelScope.launch { repository.setBmiWeightImperial(imperial) }
        }
    }
