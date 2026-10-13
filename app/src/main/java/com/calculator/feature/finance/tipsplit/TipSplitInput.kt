package com.calculator.feature.finance.tipsplit

import com.calculator.R
import com.calculator.core.common.input.InputResult
import com.calculator.core.common.input.readNumber
import com.calculator.core.common.input.valueOr
import com.calculator.core.domain.finance.TipSplitCalculator
import com.calculator.core.domain.finance.TipSplitResult
import java.util.Locale

/**
 * Reads the tip split fields as typed and splits the bill, or says
 * which field is wrong.
 *
 * @param people Head count from the stepper, which never goes below 1.
 */
internal fun tipSplitInput(
    bill: String,
    tip: String,
    people: Int,
    roundUp: Boolean,
    locale: Locale = Locale.getDefault(),
): InputResult<TipSplitResult> {
    val billAmount = readNumber(bill, R.string.tipsplit_bill, locale, min = 0.0).valueOr { return it }
    val tipAmount = readNumber(tip, R.string.tipsplit_tip_amount_input, locale, min = 0.0).valueOr { return it }
    val result = TipSplitCalculator.compute(billAmount, tipAmount, people, roundUp)
    // Two amounts close to the largest Double add up to infinity.
    return if (result.total.isFinite()) {
        InputResult.Ok(result)
    } else {
        InputResult.Invalid(R.string.tipsplit_bill, R.string.input_error_too_large)
    }
}
