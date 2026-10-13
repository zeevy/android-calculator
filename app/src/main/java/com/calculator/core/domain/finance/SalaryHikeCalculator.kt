package com.calculator.core.domain.finance

import com.calculator.core.domain.math.PercentCalculator

/**
 * Salary hike math, both ways.
 *
 *  - **With a hike**: the new salary after a hike of a given percent.
 *
 *        new = current × (1 + hike / 100)
 *        increase = new - current
 *        monthly = yearly / 12
 *
 *  - **Hike between two salaries**: the increase and the hike percent
 *    from an old salary to a new one. The percent is the plain percent
 *    change, so a pay cut gives a negative hike.
 *
 * The math is pure `Double`; rounding is the display layer's job.
 */
object SalaryHikeCalculator {
    /**
     * New salary and increase after a hike of [hikePercent] on [salary].
     *
     * @param yearly True when [salary] is per year, false when it is per month.
     * @throws IllegalArgumentException when [salary] or [hikePercent] is negative.
     * @throws ResultTooLargeException when a result overflows a Double.
     */
    fun withHike(
        salary: Double,
        hikePercent: Double,
        yearly: Boolean,
    ): SalaryHikeResult {
        require(salary >= 0) { "salary must be >= 0" }
        require(hikePercent >= 0) { "hikePercent must be >= 0" }

        val increase = PercentCalculator.percentOf(hikePercent, salary)
        val toYearly = if (yearly) 1.0 else MONTHS_PER_YEAR
        val newYearly = ((salary + increase) * toYearly).requireFinite()
        val increaseYearly = (increase * toYearly).requireFinite()
        return SalaryHikeResult(
            newMonthly = newYearly / MONTHS_PER_YEAR,
            newYearly = newYearly,
            increaseMonthly = increaseYearly / MONTHS_PER_YEAR,
            increaseYearly = increaseYearly,
        )
    }

    /**
     * Increase and hike percent from [oldSalary] to [newSalary], both in the same period.
     * A new salary of 0 gives -100 %.
     *
     * @throws IllegalArgumentException when [oldSalary] is not positive or [newSalary] is negative.
     * @throws ResultTooLargeException when the percent overflows a Double.
     */
    fun hike(
        oldSalary: Double,
        newSalary: Double,
    ): SalaryChange {
        require(oldSalary > 0) { "oldSalary must be > 0" }
        require(newSalary >= 0) { "newSalary must be >= 0" }

        return SalaryChange(
            increase = newSalary - oldSalary,
            hikePercent = PercentCalculator.percentChange(oldSalary, newSalary).requireFinite(),
        )
    }
}

/**
 * Result of [SalaryHikeCalculator.withHike].
 *
 * @property newMonthly New salary per month.
 * @property newYearly New salary per year.
 * @property increaseMonthly Increase per month.
 * @property increaseYearly Increase per year.
 */
data class SalaryHikeResult(
    val newMonthly: Double,
    val newYearly: Double,
    val increaseMonthly: Double,
    val increaseYearly: Double,
)

/**
 * Result of [SalaryHikeCalculator.hike].
 *
 * @property increase New salary minus old salary; negative for a pay cut.
 * @property hikePercent Change from the old salary, in percent; negative for a pay cut.
 */
data class SalaryChange(
    val increase: Double,
    val hikePercent: Double,
)

private const val MONTHS_PER_YEAR = 12.0
