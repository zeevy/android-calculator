package com.calculator.feature.math.percent

import com.calculator.R
import com.calculator.core.common.input.InputResult
import com.calculator.core.common.input.readNumber
import com.calculator.core.common.input.valueOr
import com.calculator.core.domain.math.PercentCalculator
import java.util.Locale

/** Label ids of the first and second field for a percentage [mode]. */
internal fun percentLabels(mode: PercentMode): Pair<Int, Int> =
    when (mode) {
        PercentMode.PercentOf -> R.string.percent_label_percent to R.string.percent_label_value
        PercentMode.WhatPercent -> R.string.percent_label_part to R.string.percent_label_whole
        PercentMode.Change -> R.string.percent_label_from to R.string.percent_label_to
    }

/**
 * Reads the two percentage fields as typed and computes the answer for
 * [mode], or says which field is wrong.
 */
internal fun percentInput(
    mode: PercentMode,
    a: String,
    b: String,
    locale: Locale = Locale.getDefault(),
): InputResult<Double> {
    val (labelA, labelB) = percentLabels(mode)
    val x = readNumber(a, labelA, locale).valueOr { return it }
    val y = readNumber(b, labelB, locale).valueOr { return it }
    // WhatPercent divides by the whole and Change by the starting value, so
    // that field can't be 0. A null result means it was 0.
    val (result, baseLabel) =
        when (mode) {
            PercentMode.PercentOf -> PercentCalculator.percentOf(x, y) to labelB
            PercentMode.WhatPercent -> (if (y == 0.0) null else PercentCalculator.whatPercent(x, y)) to labelB
            PercentMode.Change -> (if (x == 0.0) null else PercentCalculator.percentChange(x, y)) to labelA
        }
    return when {
        result == null -> InputResult.Invalid(baseLabel, R.string.input_error_zero)
        result.isFinite() -> InputResult.Ok(result)
        else -> InputResult.Invalid(baseLabel, R.string.input_error_too_large)
    }
}
