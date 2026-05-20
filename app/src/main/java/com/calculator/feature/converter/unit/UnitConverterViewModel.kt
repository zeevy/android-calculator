package com.calculator.feature.converter.unit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculator.core.common.format.NumberFormatter
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

        /**
         * Text typed into the "from" field. Marks From as the source
         * side; the next [recompute] will rewrite [toText] from this.
         */
        fun setFromInput(text: String) {
            _state.update {
                it.copy(fromText = text, lastEdited = UnitConverterUiState.PickerSide.From)
            }
            recompute()
        }

        /**
         * Text typed into the "to" field. Marks To as the source side;
         * the next [recompute] will rewrite [fromText] in reverse.
         */
        fun setToInput(text: String) {
            _state.update {
                it.copy(toText = text, lastEdited = UnitConverterUiState.PickerSide.To)
            }
            recompute()
        }

        /**
         * Swap the From and To units. Carries the previous "result"
         * (whichever side was the non-source) across to the new From
         * field, and resets the source to From so the new To recomputes
         * from it. So `1000 m → 1 km`, after swap: `1 km → 1,000 m`,
         * which is what the user expects when they tap the swap arrow
         * mid-conversion.
         */
        fun swap() {
            _state.update {
                val previousResult =
                    when (it.lastEdited) {
                        UnitConverterUiState.PickerSide.From -> it.toText
                        UnitConverterUiState.PickerSide.To -> it.fromText
                    }
                it.copy(
                    fromUnit = it.toUnit,
                    toUnit = it.fromUnit,
                    fromText = previousResult,
                    toText = "",
                    lastEdited = UnitConverterUiState.PickerSide.From,
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
         * Recompute the *non-source* side from the source side. Source
         * is [UnitConverterUiState.lastEdited]; if it's From, convert
         * From -> To and overwrite [UnitConverterUiState.toText];
         * otherwise convert To -> From and overwrite
         * [UnitConverterUiState.fromText].
         *
         * Bails to an empty target field when:
         *  - Either unit hasn't been resolved yet (transient state
         *    between `selectCategory` and the next emission).
         *  - The source text doesn't parse as a Double (empty, "-",
         *    "1.", etc.) - we keep the source field exactly as the
         *    user typed it but blank the target rather than rendering
         *    "NaN".
         */
        private fun recompute() {
            _state.update { current ->
                val from = current.fromUnit
                val to = current.toUnit
                if (from == null || to == null) {
                    return@update when (current.lastEdited) {
                        UnitConverterUiState.PickerSide.From -> current.copy(toText = "")
                        UnitConverterUiState.PickerSide.To -> current.copy(fromText = "")
                    }
                }
                when (current.lastEdited) {
                    UnitConverterUiState.PickerSide.From -> {
                        val value = current.fromText.toDoubleOrNullPermissive()
                            ?: return@update current.copy(toText = "")
                        val raw = Converter.convert(value, from, to)
                        current.copy(toText = formatResult(raw, current.precision))
                    }
                    UnitConverterUiState.PickerSide.To -> {
                        val value = current.toText.toDoubleOrNullPermissive()
                            ?: return@update current.copy(fromText = "")
                        // Reverse direction: input now lives in `to` units,
                        // result lives in `from` units. Converter is
                        // symmetric, just swap the argument order.
                        val raw = Converter.convert(value, to, from)
                        current.copy(fromText = formatResult(raw, current.precision))
                    }
                }
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
    val locale = Locale.getDefault()

    if (useScientific) {
        // Scientific notation routes through DecimalFormat with the
        // locale's symbols so the decimal separator follows convention
        // ("1.23E-5" in en-US, "1,23E-5" in de-DE). NumberFormatter
        // doesn't have a scientific mode, so we keep DecimalFormat here.
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val pattern = "0.${"#".repeat(sigFigs - 1)}E0"
        return DecimalFormat(pattern, symbols).format(value)
    }

    // floor (not toInt) because toInt truncates toward zero, which
    // miscounts magnitude for sub-unit values (log10(0.0006) is -3.2;
    // floor gives -4, the correct order of magnitude).
    val digitsAfterPoint =
        (sigFigs - 1 - floor(log10(absValue)).toInt())
            .coerceAtLeast(0)
            .coerceAtMost(MAX_DECIMAL_DIGITS)
    // Fixed-decimal path uses NumberFormatter so en-IN gets lakh
    // grouping ("12,34,567.89") and de-DE gets swapped separators
    // ("12.345,67"). Grouping enabled by default in NumberFormatter.
    return NumberFormatter.format(
        value = value,
        locale = locale,
        minFractionDigits = 0,
        maxFractionDigits = digitsAfterPoint,
    )
}

// Double-precision sane bounds for human-readable formatting.
private const val MAX_SIG_FIGS = 16
private const val MAX_DECIMAL_DIGITS = 15
private const val SCIENTIFIC_LOWER_BOUND = 1e-4
private const val SCIENTIFIC_UPPER_BOUND = 1e15
