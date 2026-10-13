package com.calculator.core.common.input

import androidx.annotation.StringRes
import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import java.util.Locale

/**
 * Outcome of reading a tool's text fields: a value, or which field is
 * wrong and why, so the screen can show the message under that field.
 */
sealed interface InputResult<out T> {
    /** All fields read fine; [value] is the calculated result. */
    data class Ok<out T>(
        val value: T,
    ) : InputResult<T>

    /**
     * One field is wrong.
     *
     * @property field String resource id of the wrong field's label.
     * @property message String resource id of the reason.
     * @property args Format arguments for [message], already formatted.
     */
    data class Invalid(
        @param:StringRes val field: Int,
        @param:StringRes val message: Int,
        val args: List<String> = emptyList(),
    ) : InputResult<Nothing>
}

/** The value, or [onInvalid] (which must return or throw) for an invalid result. */
inline fun <T> InputResult<T>.valueOr(onInvalid: (InputResult.Invalid) -> Nothing): T =
    when (this) {
        is InputResult.Ok -> value
        is InputResult.Invalid -> onInvalid(this)
    }

/**
 * Reads one number field in [locale].
 *
 * @param field String resource id of the field's label, used to place the error.
 * @param min Lowest allowed value; inclusive unless [minExclusive].
 * @param max Highest allowed value, inclusive.
 * @param whole True when the field takes only whole numbers (months, days).
 */
@Suppress("LongParameterList", "ReturnCount")
fun readNumber(
    text: String,
    @StringRes field: Int,
    locale: Locale = Locale.getDefault(),
    min: Double? = null,
    max: Double? = null,
    minExclusive: Boolean = false,
    whole: Boolean = false,
): InputResult<Double> {
    fun invalid(
        @StringRes message: Int,
        vararg args: Double,
    ) = InputResult.Invalid(field, message, args.map { NumberFormatter.format(it, locale) })

    if (text.isBlank() || text.trim() == "-") return invalid(R.string.input_error_required)
    val value = NumberFormatter.parseNumberInput(text, locale) ?: return invalid(R.string.input_error_invalid)
    if (whole && value != kotlin.math.floor(value)) return invalid(R.string.input_error_whole)
    val belowMin = min != null && (value < min || (minExclusive && value == min))
    val aboveMax = max != null && value > max
    return when {
        !belowMin && !aboveMax -> InputResult.Ok(value)
        min == 0.0 && max == null -> invalid(if (minExclusive) R.string.input_error_positive else R.string.input_error_negative)
        min != null && max != null -> invalid(R.string.input_error_range, min, max)
        min != null -> invalid(R.string.input_error_min, min)
        max != null -> invalid(R.string.input_error_max, max)
        else -> InputResult.Ok(value)
    }
}
