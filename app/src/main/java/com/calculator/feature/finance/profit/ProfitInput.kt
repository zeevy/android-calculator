package com.calculator.feature.finance.profit

import androidx.annotation.StringRes
import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.common.input.InputResult
import com.calculator.core.common.input.readNumber
import com.calculator.core.common.input.valueOr
import com.calculator.core.domain.finance.ProfitCalculator
import com.calculator.core.domain.finance.ProfitResult
import com.calculator.core.domain.finance.ResultTooLargeException
import java.util.Locale

/**
 * Reads the profit fields as typed and computes the result, or says which
 * field is wrong.
 *
 * @param second The selling price, markup or margin, as [mode] says.
 */
internal fun profitInput(
    mode: ProfitMode,
    cost: String,
    second: String,
    locale: Locale = Locale.getDefault(),
): InputResult<ProfitResult> {
    val secondLabel = profitSecondLabel(mode)
    val costPrice = readNumber(cost, R.string.profit_cost, locale, min = 0.0).valueOr { return it }
    val value = readNumber(second, secondLabel, locale, min = 0.0).valueOr { return it }
    if (mode == ProfitMode.Margin && value >= MAX_MARGIN) {
        return InputResult.Invalid(secondLabel, R.string.input_error_below, listOf(NumberFormatter.format(MAX_MARGIN, locale)))
    }
    return try {
        InputResult.Ok(
            when (mode) {
                ProfitMode.Prices -> ProfitCalculator.fromPrices(costPrice, value)
                ProfitMode.Markup -> ProfitCalculator.fromMarkup(costPrice, value)
                ProfitMode.Margin -> ProfitCalculator.fromMargin(costPrice, value)
            },
        )
    } catch (_: ResultTooLargeException) {
        // A huge markup, a margin just under 100 or a cost near 0 is the usual cause.
        InputResult.Invalid(secondLabel, R.string.input_error_too_large)
    }
}

/** Label of the field next to the cost price: the selling price, markup or margin. */
@StringRes
internal fun profitSecondLabel(mode: ProfitMode): Int =
    when (mode) {
        ProfitMode.Prices -> R.string.profit_selling
        ProfitMode.Markup -> R.string.profit_markup_input
        ProfitMode.Margin -> R.string.profit_margin_input
    }

private const val MAX_MARGIN = 100.0
