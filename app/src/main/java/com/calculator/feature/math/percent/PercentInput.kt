package com.calculator.feature.math.percent

import com.calculator.R
import com.calculator.core.common.input.InputResult
import com.calculator.core.common.input.readNumber
import com.calculator.core.common.input.valueOr
import com.calculator.core.domain.math.PercentCalculator
import java.util.Locale

/**
 * Label ids of the first and second field for a percentage [mode]:
 * 0 is "X% of Y", 1 is "X of Y is what %", 2 is "% change A to B".
 */
internal fun percentLabels(mode: Int): Pair<Int, Int> =
    when (mode) {
        0 -> R.string.percent_label_percent to R.string.percent_label_value
        1 -> R.string.percent_label_part to R.string.percent_label_whole
        else -> R.string.percent_label_from to R.string.percent_label_to
    }

/**
 * Reads the two percentage fields as typed and computes the answer for
 * [mode], or says which field is wrong.
 */
internal fun percentInput(
    mode: Int,
    a: String,
    b: String,
    locale: Locale = Locale.getDefault(),
): InputResult<Double> {
    val (labelA, labelB) = percentLabels(mode)
    val x = readNumber(a, labelA, locale).valueOr { return it }
    val y = readNumber(b, labelB, locale).valueOr { return it }
    // Mode 1 divides by the whole and mode 2 by the starting value, so
    // that field can't be 0. A null result means it was 0.
    val (result, baseLabel) =
        when (mode) {
            0 -> PercentCalculator.percentOf(x, y) to labelB
            1 -> (if (y == 0.0) null else PercentCalculator.whatPercent(x, y)) to labelB
            else -> (if (x == 0.0) null else PercentCalculator.percentChange(x, y)) to labelA
        }
    return when {
        result == null -> InputResult.Invalid(baseLabel, R.string.input_error_zero)
        result.isFinite() -> InputResult.Ok(result)
        else -> InputResult.Invalid(baseLabel, R.string.input_error_too_large)
    }
}
