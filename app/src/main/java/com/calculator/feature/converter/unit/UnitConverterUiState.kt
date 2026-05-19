package com.calculator.feature.converter.unit

import com.calculator.core.domain.converter.ConverterUnit
import com.calculator.core.domain.converter.UnitCategory

/**
 * UI state for the Unit Converter screen.
 *
 * @property category Active category (drives the unit list and the
 *   recent pair recall).
 * @property units Units in [category], in display order.
 * @property fromUnit Currently selected source unit.
 * @property toUnit Currently selected destination unit.
 * @property fromInput Raw text the user typed in the "from" field. Kept
 *   as a string so the UI can show in-progress numbers like `"-"` or
 *   `"1."` without forcing them to be valid doubles yet.
 * @property toOutput Formatted result text (empty if input is blank or
 *   unparseable).
 * @property precision Significant figures used to render [toOutput].
 * @property pickerOpen Which unit picker, if any, is open in the UI.
 */
data class UnitConverterUiState(
    val category: UnitCategory = UnitCategory.Length,
    val units: List<ConverterUnit> = emptyList(),
    val fromUnit: ConverterUnit? = null,
    val toUnit: ConverterUnit? = null,
    val fromInput: String = "",
    val toOutput: String = "",
    val precision: Int = 6,
    val pickerOpen: PickerSide? = null,
) {
    enum class PickerSide { From, To }
}
