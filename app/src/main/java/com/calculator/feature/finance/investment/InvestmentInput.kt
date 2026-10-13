package com.calculator.feature.finance.investment

import androidx.annotation.StringRes
import com.calculator.R
import com.calculator.core.common.input.InputResult
import com.calculator.core.common.input.readNumber
import com.calculator.core.common.input.valueOr
import com.calculator.core.domain.finance.CagrResult
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

/**
 * Reads the CAGR fields as typed and computes the growth rate, or says
 * which field is wrong.
 */
internal fun cagrInput(
    start: String,
    end: String,
    years: String,
    locale: Locale = Locale.getDefault(),
): InputResult<CagrResult> {
    val from = readNumber(start, R.string.investment_start, locale, min = 0.0, minExclusive = true).valueOr { return it }
    val to = readNumber(end, R.string.investment_end, locale, min = 0.0).valueOr { return it }
    val tenure =
        readNumber(years, R.string.investment_years, locale, min = 0.0, minExclusive = true).valueOr { return it }
    return try {
        InputResult.Ok(InvestmentCalculator.cagr(from, to, tenure))
    } catch (_: ResultTooLargeException) {
        // A tiny tenure with a large growth is the usual cause.
        InputResult.Invalid(R.string.investment_years, R.string.input_error_too_large)
    }
}

/** Label of the amount field: the principal for a lump sum, the monthly amount for a SIP. */
@StringRes
internal fun investmentAmountLabel(lumpSum: Boolean): Int =
    if (lumpSum) R.string.investment_principal else R.string.investment_monthly
