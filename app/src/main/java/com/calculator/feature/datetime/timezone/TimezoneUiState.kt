package com.calculator.feature.datetime.timezone

import java.time.LocalDateTime

/**
 * UI state for the timezone converter.
 *
 * Bidirectional edit: both [fromTime] and [toTime] are editable. The
 * one the user last touched is the [lastEdited] "source", and the
 * other is recomputed by the ViewModel via
 * [com.calculator.core.domain.datetime.TimezoneConverter.convert].
 *
 * @property fromZone IANA zone id for the source side.
 * @property fromTime Wall-clock time in [fromZone].
 * @property toZone IANA zone id for the target side.
 * @property toTime Wall-clock time in [toZone].
 * @property lastEdited Which side is the source - the OTHER one
 *   recomputes when units, zones, or times change.
 * @property pickerOpen Which picker, if any, is currently visible.
 */
data class TimezoneUiState(
    val fromZone: String = "UTC",
    val fromTime: LocalDateTime = LocalDateTime.now(),
    val toZone: String = "Asia/Kolkata",
    val toTime: LocalDateTime = LocalDateTime.now(),
    val lastEdited: Side = Side.From,
    val pickerOpen: Picker? = null,
) {
    /** Which side the user last edited. */
    enum class Side { From, To }

    /** Active modal sub-picker, if any. */
    sealed interface Picker {
        data object FromZone : Picker
        data object ToZone : Picker
        data object FromDate : Picker
        data object FromTime : Picker
        data object ToDate : Picker
        data object ToTime : Picker
    }
}
