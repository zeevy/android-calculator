package com.calculator.feature.datetime.datediff

import com.calculator.R
import com.calculator.core.common.input.InputResult
import com.calculator.core.common.input.readNumber
import com.calculator.core.common.input.valueOr
import com.calculator.core.domain.datetime.DateDiffCalculator
import java.time.LocalDate
import java.util.Locale

/** Largest offset either way: 100,000 days, about 273 years. */
internal const val MAX_OFFSET_DAYS = 100_000

/**
 * Reads the typed day offset and adds it to [start], or says the
 * offset field is wrong.
 */
internal fun dateOffsetInput(
    start: LocalDate,
    days: String,
    locale: Locale = Locale.getDefault(),
): InputResult<LocalDate> {
    val offset =
        readNumber(
            days,
            R.string.datediff_offset,
            locale,
            min = -MAX_OFFSET_DAYS.toDouble(),
            max = MAX_OFFSET_DAYS.toDouble(),
            whole = true,
        ).valueOr { return it }
    return InputResult.Ok(DateDiffCalculator.addOffset(start, offset.toInt()))
}
