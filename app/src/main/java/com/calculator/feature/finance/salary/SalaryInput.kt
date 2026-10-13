package com.calculator.feature.finance.salary

import com.calculator.R
import com.calculator.core.common.input.InputResult
import com.calculator.core.common.input.readNumber
import com.calculator.core.common.input.valueOr
import com.calculator.core.domain.finance.ResultTooLargeException
import com.calculator.core.domain.finance.SalaryChange
import com.calculator.core.domain.finance.SalaryHikeCalculator
import com.calculator.core.domain.finance.SalaryHikeResult
import java.util.Locale

/**
 * Reads the current salary and hike as typed and computes the new
 * salary, or says which field is wrong.
 *
 * @param yearly True when the salary is typed per year, false per month.
 */
internal fun newSalaryInput(
    salary: String,
    hike: String,
    yearly: Boolean,
    locale: Locale = Locale.getDefault(),
): InputResult<SalaryHikeResult> {
    val current = readNumber(salary, R.string.salary_current, locale, min = 0.0).valueOr { return it }
    val percent = readNumber(hike, R.string.salary_hike_input, locale, min = 0.0).valueOr { return it }
    return try {
        InputResult.Ok(SalaryHikeCalculator.withHike(current, percent, yearly))
    } catch (_: ResultTooLargeException) {
        InputResult.Invalid(R.string.salary_current, R.string.input_error_too_large)
    }
}

/**
 * Reads the old and new salary as typed and computes the hike, or says
 * which field is wrong.
 */
internal fun hikeInput(
    oldSalary: String,
    newSalary: String,
    locale: Locale = Locale.getDefault(),
): InputResult<SalaryChange> {
    val from = readNumber(oldSalary, R.string.salary_old, locale, min = 0.0, minExclusive = true).valueOr { return it }
    val to = readNumber(newSalary, R.string.salary_new, locale, min = 0.0).valueOr { return it }
    return try {
        InputResult.Ok(SalaryHikeCalculator.hike(from, to))
    } catch (_: ResultTooLargeException) {
        // A tiny old salary is the usual cause.
        InputResult.Invalid(R.string.salary_old, R.string.input_error_too_large)
    }
}
