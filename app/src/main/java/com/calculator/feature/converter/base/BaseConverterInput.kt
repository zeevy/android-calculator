package com.calculator.feature.converter.base

import androidx.annotation.StringRes
import com.calculator.R
import com.calculator.core.common.input.InputResult
import com.calculator.core.domain.math.BaseConverter

/**
 * Reads the base field the user is typing in, or says why it is wrong.
 * Underscores are allowed as digit group marks.
 *
 * @param radix Base of the field: 2, 8, 10 or 16.
 */
internal fun baseConverterInput(
    text: String,
    radix: Int,
): InputResult<Long> {
    val field = baseLabel(radix)
    val digits = text.trim().replace("_", "")
    if (digits.isEmpty()) return InputResult.Invalid(field, R.string.input_error_required)
    val value = BaseConverter.parse(digits, radix)
    return when {
        value != null -> InputResult.Ok(value)
        // Only valid digits but no Long holds it.
        digits.all { Character.digit(it, radix) >= 0 } -> InputResult.Invalid(field, R.string.input_error_too_large)
        else -> InputResult.Invalid(field, R.string.input_error_invalid)
    }
}

/**
 * What the field for [radix] shows. The field being typed in keeps its
 * text as typed, so mid-edit text like "" or "1_0" stays put. The others
 * show [lastValue], the last value that parsed, in their own base.
 */
internal fun baseFieldText(
    radix: Int,
    sourceRadix: Int,
    sourceText: String,
    lastValue: Long,
): String = if (radix == sourceRadix) sourceText else BaseConverter.format(lastValue, radix)

/** String resource id of the label of the field for [radix]. */
@StringRes
private fun baseLabel(radix: Int): Int =
    when (radix) {
        BaseConverter.BIN -> R.string.basecalc_bin
        BaseConverter.OCT -> R.string.basecalc_oct
        BaseConverter.HEX -> R.string.basecalc_hex
        else -> R.string.basecalc_dec
    }
