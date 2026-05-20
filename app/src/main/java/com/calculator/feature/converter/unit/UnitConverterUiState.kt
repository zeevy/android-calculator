package com.calculator.feature.converter.unit

import com.calculator.core.domain.converter.ConverterUnit
import com.calculator.core.domain.converter.UnitCategory

/**
 * UI state for the Unit Converter screen.
 *
 * Bidirectional edit: both [fromText] and [toText] are user-editable.
 * Whichever side the user typed into last is the [lastEdited] "source"
 * and the other side displays the computed conversion. On every
 * input / unit change we recompute the *other* field from the source.
 *
 * @property category Active category (drives the unit list and the
 *   recent pair recall).
 * @property units Units in [category], in display order.
 * @property fromUnit Currently selected source unit.
 * @property toUnit Currently selected destination unit.
 * @property fromText Text shown in the "from" field. Either the user's
 *   in-progress input (when [lastEdited] is [PickerSide.From]) or the
 *   formatted conversion of [toText] (when [lastEdited] is
 *   [PickerSide.To]). Kept as a string so in-progress numbers like
 *   `"-"` or `"1."` survive without being forced to valid doubles yet.
 * @property toText Symmetric to [fromText] for the "to" side.
 * @property lastEdited Which field the user typed into last. The OTHER
 *   field is the one we recompute on every state change.
 * @property precision Significant figures used to render the computed
 *   side.
 * @property pickerOpen Which unit picker, if any, is open in the UI.
 */
data class UnitConverterUiState(
    val category: UnitCategory = UnitCategory.Length,
    val units: List<ConverterUnit> = emptyList(),
    val fromUnit: ConverterUnit? = null,
    val toUnit: ConverterUnit? = null,
    val fromText: String = "",
    val toText: String = "",
    val lastEdited: PickerSide = PickerSide.From,
    val precision: Int = 6,
    val pickerOpen: PickerSide? = null,
) {
    enum class PickerSide { From, To }
}
