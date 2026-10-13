package com.calculator.feature.datetime.timezone

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculator.core.data.settings.SettingsRepository
import com.calculator.core.domain.datetime.TimezoneConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Backs [TimezoneScreen].
 *
 * State machine
 * -------------
 * Two sides (From, To). [TimezoneUiState.lastEdited] tracks the one
 * the user last touched. Any change to a zone, date, or time on either
 * side triggers [recompute] on the *other* side.
 *
 * Default zones load from [SettingsRepository] (last-used (from, to)
 * pair, falling back to UTC -> Asia/Kolkata for first launch). Zone
 * changes persist immediately so the next visit lands on the same
 * pair.
 */
@HiltViewModel
class TimezoneViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val savedStateHandle: SavedStateHandle,
        private val clock: Clock,
    ) : ViewModel() {
        // Times the user picked come back from SavedStateHandle after
        // process death; the zones come back from settings in init.
        private val savedFromTime: LocalDateTime? = savedStateHandle[KEY_FROM_TIME]

        private val _state =
            MutableStateFlow(
                // Placeholder until init knows the From zone and sets the
                // current time there.
                TimezoneUiState(
                    fromTime = savedFromTime ?: LocalDateTime.now(clock),
                    toTime = savedStateHandle[KEY_TO_TIME] ?: LocalDateTime.now(clock),
                    lastEdited =
                        savedStateHandle.get<String>(KEY_LAST_EDITED)?.let { TimezoneUiState.Side.valueOf(it) }
                            ?: TimezoneUiState.Side.From,
                ),
            )
        val state: StateFlow<TimezoneUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                val saved = settings.settings.firstOrNull()
                val from = saved?.lastTzFromZone ?: "UTC"
                val to = saved?.lastTzToZone ?: "Asia/Kolkata"
                _state.update { current ->
                    // Start from "now" in the From zone, not the device's wall
                    // time labelled as the From zone.
                    val fromZoneId = runCatching { ZoneId.of(from) }.getOrNull()
                    val now = if (savedFromTime == null &&
                        fromZoneId != null
                    ) {
                        LocalDateTime.now(clock.withZone(fromZoneId))
                    } else {
                        current.fromTime
                    }
                    current.copy(fromZone = from, toZone = to, fromTime = now)
                }
                recompute()
            }
        }

        fun setFromZone(zone: String) {
            _state.update { it.copy(fromZone = zone, pickerOpen = null) }
            recordRecent()
            recompute()
        }

        fun setToZone(zone: String) {
            _state.update { it.copy(toZone = zone, pickerOpen = null) }
            recordRecent()
            recompute()
        }

        /** Update the From-side date (keeps the existing time-of-day). */
        fun setFromDate(date: LocalDate) {
            _state.update {
                it.copy(
                    fromTime = LocalDateTime.of(date, it.fromTime.toLocalTime()),
                    lastEdited = TimezoneUiState.Side.From,
                    pickerOpen = null,
                )
            }
            recompute()
        }

        /** Update the From-side time-of-day (keeps the existing date). */
        fun setFromTime(time: LocalTime) {
            _state.update {
                it.copy(
                    fromTime = LocalDateTime.of(it.fromTime.toLocalDate(), time),
                    lastEdited = TimezoneUiState.Side.From,
                    pickerOpen = null,
                )
            }
            recompute()
        }

        fun setToDate(date: LocalDate) {
            _state.update {
                it.copy(
                    toTime = LocalDateTime.of(date, it.toTime.toLocalTime()),
                    lastEdited = TimezoneUiState.Side.To,
                    pickerOpen = null,
                )
            }
            recompute()
        }

        fun setToTime(time: LocalTime) {
            _state.update {
                it.copy(
                    toTime = LocalDateTime.of(it.toTime.toLocalDate(), time),
                    lastEdited = TimezoneUiState.Side.To,
                    pickerOpen = null,
                )
            }
            recompute()
        }

        /** Swap the From and To zones AND the times that go with them. */
        fun swap() {
            _state.update {
                it.copy(
                    fromZone = it.toZone,
                    toZone = it.fromZone,
                    fromTime = it.toTime,
                    toTime = it.fromTime,
                    lastEdited = TimezoneUiState.Side.From,
                )
            }
            recordRecent()
            recompute()
        }

        /** Open a sub-picker (zone list, date dialog, or time dialog). */
        fun openPicker(picker: TimezoneUiState.Picker) {
            _state.update { it.copy(pickerOpen = picker) }
        }

        fun dismissPicker() {
            _state.update { it.copy(pickerOpen = null) }
        }

        private fun recordRecent() {
            val snap = _state.value
            viewModelScope.launch {
                settings.setLastTzPair(snap.fromZone, snap.toZone)
            }
        }

        /**
         * Rewrite the non-source side from the source side. If the
         * source is From, convert fromTime From->To and overwrite
         * [TimezoneUiState.toTime]; otherwise convert in reverse.
         */
        private fun recompute() {
            recomputeState()
            val saved = _state.value
            savedStateHandle[KEY_FROM_TIME] = saved.fromTime
            savedStateHandle[KEY_TO_TIME] = saved.toTime
            savedStateHandle[KEY_LAST_EDITED] = saved.lastEdited.name
        }

        private fun recomputeState() {
            _state.update { current ->
                val fromZone = runCatching { ZoneId.of(current.fromZone) }.getOrNull() ?: return@update current
                val toZone = runCatching { ZoneId.of(current.toZone) }.getOrNull() ?: return@update current
                when (current.lastEdited) {
                    TimezoneUiState.Side.From -> {
                        val newTo = TimezoneConverter.convert(current.fromTime, fromZone, toZone)
                        current.copy(toTime = newTo)
                    }
                    TimezoneUiState.Side.To -> {
                        val newFrom = TimezoneConverter.convert(current.toTime, toZone, fromZone)
                        current.copy(fromTime = newFrom)
                    }
                }
            }
        }

        private companion object {
            const val KEY_FROM_TIME = "timezone.fromTime"
            const val KEY_TO_TIME = "timezone.toTime"
            const val KEY_LAST_EDITED = "timezone.lastEdited"
        }
    }
