package com.calculator.feature.finance.gst

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

/** Remembers the GST screen's last intra/inter choice and rate. */
@HiltViewModel
class GstViewModel
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) : ViewModel() {
        /** Saved settings; the screen reads `gstIntraState` and `gstRate`. */
        val settings: StateFlow<UserSettings> =
            repository.settings.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = UserSettings(),
            )

        /** Saves whether the sale is within one state. */
        fun setIntraState(intra: Boolean) {
            viewModelScope.launch { repository.setGstIntraState(intra) }
        }

        /** Saves the GST rate as typed, in percent. */
        fun setRate(rate: String) {
            viewModelScope.launch { repository.setGstRate(rate) }
        }
    }
