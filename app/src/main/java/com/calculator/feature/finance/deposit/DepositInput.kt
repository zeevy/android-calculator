package com.calculator.feature.finance.deposit

import com.calculator.R
import com.calculator.core.common.input.InputResult
import com.calculator.core.common.input.readNumber
import com.calculator.core.common.input.valueOr
import com.calculator.core.domain.finance.DepositCalculator
import com.calculator.core.domain.finance.DepositResult
import com.calculator.core.domain.finance.PpfResult
import com.calculator.core.domain.finance.ResultTooLargeException
import java.util.Locale

/** Reads the FD fields as typed and computes the maturity, or says which field is wrong. */
internal fun fdInput(
    fields: FdFields,
    locale: Locale = Locale.getDefault(),
): InputResult<DepositResult> {
    val amount = readNumber(fields.amount, R.string.deposit_amount, locale, min = 0.0).valueOr { return it }
    val rate = readNumber(fields.rate, R.string.deposit_rate, locale, min = 0.0).valueOr { return it }
    val months = readFdMonths(fields, locale).valueOr { return it }
    return tooLargeAsError {
        DepositCalculator.fixedDeposit(amount, rate, months, fields.compounding.timesPerYear)
    }
}

// The FD tenure is typed as years plus months; together they must be more than 0.
private fun readFdMonths(
    fields: FdFields,
    locale: Locale,
): InputResult<Int> {
    val years =
        readNumber(fields.years, R.string.deposit_years, locale, min = 0.0, max = MAX_FD_YEARS, whole = true)
            .valueOr { return it }
    val months =
        readNumber(fields.months, R.string.deposit_months, locale, min = 0.0, max = MAX_EXTRA_MONTHS, whole = true)
            .valueOr { return it }
    val tenure = (years * MONTHS_PER_YEAR + months).toInt()
    return if (tenure == 0) {
        InputResult.Invalid(R.string.deposit_months, R.string.input_error_positive)
    } else {
        InputResult.Ok(tenure)
    }
}

/** Reads the RD fields as typed and computes the maturity, or says which field is wrong. */
internal fun rdInput(
    fields: RdFields,
    locale: Locale = Locale.getDefault(),
): InputResult<DepositResult> {
    val amount = readNumber(fields.amount, R.string.deposit_monthly, locale, min = 0.0).valueOr { return it }
    val rate = readNumber(fields.rate, R.string.deposit_rate, locale, min = 0.0).valueOr { return it }
    val months =
        readNumber(fields.months, R.string.deposit_tenure, locale, min = 1.0, max = MAX_RD_MONTHS, whole = true)
            .valueOr { return it }
    return tooLargeAsError { DepositCalculator.recurringDeposit(amount, rate, months.toInt()) }
}

/** Reads the PPF fields as typed and computes the maturity, or says which field is wrong. */
internal fun ppfInput(
    fields: PpfFields,
    locale: Locale = Locale.getDefault(),
): InputResult<PpfResult> {
    val amount =
        readNumber(
            fields.amount,
            R.string.deposit_yearly,
            locale,
            min = DepositCalculator.PPF_MIN_YEARLY_DEPOSIT,
            max = DepositCalculator.PPF_MAX_YEARLY_DEPOSIT,
        ).valueOr { return it }
    val rate = readNumber(fields.rate, R.string.deposit_rate, locale, min = 0.0).valueOr { return it }
    return tooLargeAsError { DepositCalculator.ppf(amount, rate) }
}

// The tenures are capped, so a huge rate is the usual cause of an overflow.
private inline fun <T> tooLargeAsError(compute: () -> T): InputResult<T> =
    try {
        InputResult.Ok(compute())
    } catch (_: ResultTooLargeException) {
        InputResult.Invalid(R.string.deposit_rate, R.string.input_error_too_large)
    }

private const val MONTHS_PER_YEAR = 12

// FDs run up to 10 years at most banks; 100 keeps any real case and
// keeps the tenure in months well inside an Int.
private const val MAX_FD_YEARS = 100.0
private const val MAX_EXTRA_MONTHS = 11.0

// Same cap as the loan tenure: 50 years of monthly deposits.
private const val MAX_RD_MONTHS = 600.0
