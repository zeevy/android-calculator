package com.calculator.feature.finance.loan

import com.calculator.R
import com.calculator.core.common.input.InputResult
import com.calculator.core.common.input.readNumber
import com.calculator.core.common.input.valueOr
import com.calculator.core.domain.finance.EmiCalculator
import com.calculator.core.domain.finance.EmiResult
import com.calculator.core.domain.finance.ResultTooLargeException
import java.util.Locale

/** Longest tenure the estimator accepts: 50 years. */
internal const val MAX_TENURE_MONTHS = 600

/**
 * Reads the loan fields as typed and computes the EMI, or says which
 * field is wrong.
 */
internal fun loanInput(
    principal: String,
    rate: String,
    months: String,
    locale: Locale = Locale.getDefault(),
): InputResult<EmiResult> {
    val amount = readNumber(principal, R.string.loan_loan_amount, locale, min = 0.0, minExclusive = true).valueOr { return it }
    val annualRate = readNumber(rate, R.string.loan_annual_rate, locale, min = 0.0).valueOr { return it }
    val tenure =
        readNumber(months, R.string.loan_tenure, locale, min = 1.0, max = MAX_TENURE_MONTHS.toDouble(), whole = true)
            .valueOr { return it }
    return try {
        InputResult.Ok(EmiCalculator.emi(amount, annualRate, tenure.toInt()))
    } catch (_: ResultTooLargeException) {
        InputResult.Invalid(R.string.loan_annual_rate, R.string.input_error_too_large)
    }
}
