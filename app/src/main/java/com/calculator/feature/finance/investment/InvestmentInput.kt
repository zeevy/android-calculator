package com.calculator.feature.finance.investment

import androidx.annotation.StringRes
import com.calculator.R
import com.calculator.core.common.input.InputResult
import com.calculator.core.common.input.readNumber
import com.calculator.core.common.input.valueOr
import com.calculator.core.domain.finance.InvestmentCalculator
import com.calculator.core.domain.finance.InvestmentResult
import com.calculator.core.domain.finance.ResultTooLargeException
import java.util.Locale

/**
 * Reads the investment fields as typed and computes the maturity, or
 * says which field is wrong.
 *
 * @param lumpSum True for a one-time principal, false for a monthly SIP.
 */
internal fun investmentInput(
    lumpSum: Boolean,
    amount: String,
    rate: String,
    years: String,
    locale: Locale = Locale.getDefault(),
): InputResult<InvestmentResult> {
    val money = readNumber(amount, investmentAmountLabel(lumpSum), locale, min = 0.0).valueOr { return it }
    val annualRate = readNumber(rate, R.string.investment_rate, locale, min = 0.0).valueOr { return it }
    val tenure =
        readNumber(years, R.string.investment_years, locale, min = 0.0, minExclusive = true).valueOr { return it }
    return try {
        InputResult.Ok(
            if (lumpSum) {
                InvestmentCalculator.lumpSum(money, annualRate, tenure, timesPerYear = 1)
            } else {
                InvestmentCalculator.sip(money, annualRate, tenure)
            },
        )
    } catch (_: ResultTooLargeException) {
        // Tenure has no upper limit, so a very long tenure is the usual cause.
        InputResult.Invalid(R.string.investment_years, R.string.input_error_too_large)
    }
}

/** Label of the amount field: the principal for a lump sum, the monthly amount for a SIP. */
@StringRes
internal fun investmentAmountLabel(lumpSum: Boolean): Int =
    if (lumpSum) R.string.investment_principal else R.string.investment_monthly
