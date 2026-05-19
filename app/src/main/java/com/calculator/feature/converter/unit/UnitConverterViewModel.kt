package com.calculator.feature.converter.unit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculator.core.data.converter.UnitConverterRepository
import com.calculator.core.data.settings.SettingsRepository
import com.calculator.core.domain.converter.ConversionTable
import com.calculator.core.domain.converter.Converter
import com.calculator.core.domain.converter.ConverterUnit
import com.calculator.core.domain.converter.UnitCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10

/**
 * State holder for the Unit Converter.
 *
 * The flow on each interaction:
 *  1. User selects a category, the "from" unit, or the "to" unit.
 *  2. User types a number into the "from" field.
 *  3. ViewModel runs [Converter.convert] using `Double` and formats the
 *     result with the user's significant-figures preference.
 *  4. Repository records the (from, to) pair so the next visit to that
 *     category starts on the user's preferred pairing.
 */
@HiltViewModel
class UnitConverterViewModel
    @Inject
    constructor(
        private val repository: UnitConverterRepository,
        private val settings: SettingsRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(UnitConverterUiState())
        val state: StateFlow<UnitConverterUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                settings.settings.collect { snapshot ->
                    _state.update { it.copy(precision = snapshot.precision) }
                    // Recompute output to pick up the new precision.
                    recompute()
                }
            }
        }

        init {
            // Bootstrap with the default category (Length) and apply the
            // saved recent pair if any.
            viewModelScope.launch { selectCategory(UnitCategory.Length) }
        }

        /**
         * Switch to [category], loading its units and applying any
         * saved "recent" pair the user previously picked.
         *
         * Resolution rules:
         *  - "From" unit: use the saved symbol if it still exists in
         *    the category's table, else fall back to the first unit
         *    (the canonical, e.g. metre/litre/kilogram).
         *  - "To" unit: use the saved symbol if it exists, else the
         *    second unit, else the first (some categories have only
         *    one common-pair member if a future trim happens).
         *
         * Recompute is dispatched after the state is updated so the
         * output reflects the new units immediately.
         */
        fun selectCategory(category: UnitCategory) {
            viewModelScope.launch {
                val units = ConversionTable.unitsFor(category)
                val saved = repository.recent(category)
                // `firstOrNull` (not single()) because the saved symbol
                // may not be in the current unit list if the table has
                // been trimmed since the user last selected.
                val from =
                    saved?.first?.let { sym -> units.firstOrNull { it.symbol == sym } }
                        ?: units.first()
                val to =
                    saved?.second?.let { sym -> units.firstOrNull { it.symbol == sym } }
                        ?: units.getOrNull(1)
                        ?: units.first()
                _state.update {
                    it.copy(
                        category = category,
                        units = units,
                        fromUnit = from,
                        toUnit = to,
                    )
                }
                recompute()
            }
        }

        fun setFromUnit(unit: ConverterUnit) {
            _state.update { it.copy(fromUnit = unit, pickerOpen = null) }
            recordRecent()
            recompute()
        }

        fun setToUnit(unit: ConverterUnit) {
            _state.update { it.copy(toUnit = unit, pickerOpen = null) }
            recordRecent()
            recompute()
        }

        fun setFromInput(text: String) {
            _state.update { it.copy(fromInput = text) }
            recompute()
        }

        fun swap() {
            _state.update {
                it.copy(
                    fromUnit = it.toUnit,
                    toUnit = it.fromUnit,
                    fromInput = it.toOutput,
                )
            }
            recordRecent()
            recompute()
        }

        fun openPicker(side: UnitConverterUiState.PickerSide) {
            _state.update { it.copy(pickerOpen = side) }
        }

        fun dismissPicker() {
            _state.update { it.copy(pickerOpen = null) }
        }

        private fun recordRecent() {
            val snap = _state.value
            val from = snap.fromUnit?.symbol ?: return
            val to = snap.toUnit?.symbol ?: return
            viewModelScope.launch { repository.record(snap.category, from, to) }
        }

        /**
         * Recompute [UnitConverterUiState.toOutput] from the current
         * input and unit selection.
         *
         * Bails to an empty output when:
         *  - Either unit hasn't been resolved yet (transient state
         *    between `selectCategory` and the next emission).
         *  - The input doesn't parse as a Double (empty, "-", "1.",
         *    etc.) - we keep the field as the user typed it but show
         *    no output rather than rendering "NaN".
         */
        private fun recompute() {
            _state.update { current ->
                val from = current.fromUnit
                val to = current.toUnit
                if (from == null || to == null) return@update current.copy(toOutput = "")
                val value = current.fromInput.toDoubleOrNullPermissive()
                if (value == null) return@update current.copy(toOutput = "")
                val raw = Converter.convert(value, from, to)
                current.copy(toOutput = formatResult(raw, current.precision))
            }
        }
    }

/** "1.", "-", and "" all parse to null without raising. */
private fun String.toDoubleOrNullPermissive(): Double? {
    if (isBlank() || this == "-" || this == "." || this == "-.") return null
    return toDoubleOrNull()
}

/**
 * Renders [value] to a string with [significantFigures] precision,
 * trimming meaningless trailing zeros and using fixed-decimal notation
 * for any magnitude small enough to be readable that way.
 *
 * Scientific notation kicks in only for extreme magnitudes (< 1e-4 or
 * >= 1e15) where the fixed-decimal form would be either a wall of
 * leading zeros or unreadably long.
 */
internal fun formatResult(value: Double, significantFigures: Int): String {
    if (value == 0.0 || !value.isFinite()) return if (value.isFinite()) "0" else "—"
    val absValue = abs(value)
    val sigFigs = significantFigures.coerceIn(1, MAX_SIG_FIGS)
    val useScientific = absValue < SCIENTIFIC_LOWER_BOUND || absValue >= SCIENTIFIC_UPPER_BOUND
    val symbols = DecimalFormatSymbols(Locale.US)
    val format =
        if (useScientific) {
            DecimalFormat("0.${"#".repeat(sigFigs - 1)}E0", symbols)
        } else {
            // floor (not toInt) because toInt truncates toward zero, which
            // miscounts magnitude for sub-unit values (log10(0.0006) is
            // -3.2; floor gives -4 which is the correct order of
            // magnitude).
            val digitsAfterPoint =
                (sigFigs - 1 - floor(log10(absValue)).toInt())
                    .coerceAtLeast(0)
                    .coerceAtMost(MAX_DECIMAL_DIGITS)
            DecimalFormat("0.${"#".repeat(digitsAfterPoint)}", symbols)
        }
    return format.format(value)
}

// Double-precision sane bounds for human-readable formatting.
private const val MAX_SIG_FIGS = 16
private const val MAX_DECIMAL_DIGITS = 15
private const val SCIENTIFIC_LOWER_BOUND = 1e-4
private const val SCIENTIFIC_UPPER_BOUND = 1e15
