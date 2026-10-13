package com.calculator.feature.finance.discount

import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.common.input.InputResult
import com.calculator.core.common.input.readNumber
import com.calculator.core.common.input.valueOr
import com.calculator.core.domain.finance.DiscountCalculator
import com.calculator.core.domain.finance.DiscountResult
import java.util.Locale

/**
 * The discount fields once read: the result and the text the percent
 * and final price fields show.
 *
 * @property percentText Percent field text: as typed when percent is the source, else the computed percent.
 * @property finalText Final price field text: as typed when final price is the source, else the computed price.
 */
internal data class DiscountInput(
    val result: DiscountResult,
    val percentText: String,
    val finalText: String,
)

/**
 * Reads the discount fields as typed and computes the result, or says
 * which field is wrong. Of percent and final price, the one typed in
 * last is the source and the other is computed from it and the MRP.
 *
 * @param finalIsSource True when the user typed in the final price field last.
 */
internal fun discountInput(
    mrp: String,
    percentOff: String,
    finalPrice: String,
    finalIsSource: Boolean,
    locale: Locale = Locale.getDefault(),
): InputResult<DiscountInput> {
    // Working out the percent divides by the MRP, so it must be above 0 then.
    val price =
        readNumber(mrp, R.string.discount_mrp, locale, min = 0.0, minExclusive = finalIsSource)
            .valueOr { return it }
    val result =
        if (finalIsSource) {
            val final = readNumber(finalPrice, R.string.discount_final, locale, min = 0.0, max = price).valueOr { return it }
            DiscountCalculator.reverse(price, final)
        } else {
            val percent = readNumber(percentOff, R.string.discount_percent, locale, min = 0.0, max = 100.0).valueOr { return it }
            DiscountCalculator.forward(price, percent)
        }
    return InputResult.Ok(
        DiscountInput(
            result = result,
            percentText = if (finalIsSource) NumberFormatter.formatForInput(result.percentOff, locale) else percentOff,
            finalText = if (finalIsSource) finalPrice else NumberFormatter.formatForInput(result.finalPrice, locale),
        ),
    )
}
