package com.calculator.feature.finance.gst

import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.common.input.InputResult
import com.calculator.core.common.input.readNumber
import com.calculator.core.common.input.valueOr
import com.calculator.core.domain.finance.GstCalculator
import com.calculator.core.domain.finance.GstResult
import java.util.Locale

/**
 * The GST fields once read: the split and the text both amount fields show.
 *
 * @property netText Net field text: as typed when net is the source, else the computed net.
 * @property grossText Gross field text: as typed when gross is the source, else the computed gross.
 */
internal data class GstInput(
    val result: GstResult,
    val netText: String,
    val grossText: String,
)

/**
 * Reads the GST fields as typed and computes the split, or says which
 * field is wrong. The amount field typed in last is the source and the
 * other amount is computed from it, so the other field's text is not read.
 *
 * @param grossIsSource True when the user typed in the gross field last.
 */
internal fun gstInput(
    net: String,
    gross: String,
    rate: String,
    intraState: Boolean,
    grossIsSource: Boolean,
    locale: Locale = Locale.getDefault(),
): InputResult<GstInput> {
    val sourceField = if (grossIsSource) R.string.gst_amount_gross else R.string.gst_amount_net
    val amount = readNumber(if (grossIsSource) gross else net, sourceField, locale, min = 0.0).valueOr { return it }
    val ratePercent = readNumber(rate, R.string.gst_rate, locale, min = 0.0).valueOr { return it }
    val result =
        if (grossIsSource) {
            GstCalculator.reverse(amount, ratePercent, intraState)
        } else {
            GstCalculator.forward(amount, ratePercent, intraState)
        }
    // A huge net times a huge rate overflows to infinity.
    if (!result.gross.isFinite()) return InputResult.Invalid(sourceField, R.string.input_error_too_large)
    return InputResult.Ok(
        GstInput(
            result = result,
            netText = if (grossIsSource) NumberFormatter.formatForInput(result.net, locale) else net,
            grossText = if (grossIsSource) gross else NumberFormatter.formatForInput(result.gross, locale),
        ),
    )
}
