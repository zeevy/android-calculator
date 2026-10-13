package com.calculator.core.domain.finance

import com.calculator.core.domain.math.PercentCalculator
import kotlin.math.pow

/**
 * Two flavours of long-horizon return math.
 *
 *  - **SIP (Systematic Investment Plan)**: a fixed [monthlyAmount] is
 *    contributed at the start of every month at an annual rate of
 *    [annualRatePercent] for [years] years; we compound monthly.
 *    Maturity uses the annuity-due future-value formula:
 *
 *        FV = P × [((1+r)^n - 1) / r] × (1+r)
 *
 *    where `r = annualRate/12` and `n = years*12`.
 *
 *  - **Lump-sum compound interest**: a single [principal] compounded
 *    [timesPerYear] times per year for [years] years:
 *
 *        A = P × (1 + r/n)^(n*t)
 *
 * Both return an [InvestmentResult] reporting the maturity value, the
 * total amount invested, and the gains (maturity - invested).
 *
 *  - **CAGR**: the reverse of the lump sum. The yearly rate that grows
 *    a start value into an end value over a number of years:
 *
 *        CAGR = (end / start)^(1 / years) - 1
 *
 * The math is pure `Double`; rounding to the user's preferred precision
 * is the display layer's responsibility.
 */
object InvestmentCalculator {
    /**
     * Value of a monthly SIP of [monthlyAmount] for [years] at [annualRatePercent],
     * each payment made at the start of its month.
     *
     * @throws IllegalArgumentException when an amount or rate is negative or [years] is not positive.
     * @throws ResultTooLargeException when the value overflows a Double.
     */
    fun sip(
        monthlyAmount: Double,
        annualRatePercent: Double,
        years: Double,
    ): InvestmentResult {
        require(monthlyAmount >= 0) { "monthlyAmount must be >= 0" }
        require(annualRatePercent >= 0) { "annualRatePercent must be >= 0" }
        require(years > 0) { "years must be > 0" }

        val months = years * MONTHS_PER_YEAR
        val monthlyRate = annualRatePercent / PERCENT_BASE / MONTHS_PER_YEAR
        val maturity =
            if (monthlyRate == 0.0) {
                monthlyAmount * months
            } else {
                monthlyAmount *
                    ((1.0 + monthlyRate).pow(months) - 1.0) / monthlyRate *
                    (1.0 + monthlyRate)
            }
        val invested = monthlyAmount * months
        return InvestmentResult(
            maturity = maturity.requireFinite(),
            invested = invested.requireFinite(),
            gains = maturity - invested,
        )
    }

    /**
     * Value of [principal] invested once for [years] at [annualRatePercent],
     * compounded [timesPerYear] times a year.
     *
     * @throws IllegalArgumentException when an amount or rate is negative, [years] is not positive
     *   or [timesPerYear] is below 1.
     * @throws ResultTooLargeException when the value overflows a Double.
     */
    fun lumpSum(
        principal: Double,
        annualRatePercent: Double,
        years: Double,
        timesPerYear: Int = 1,
    ): InvestmentResult {
        require(principal >= 0) { "principal must be >= 0" }
        require(annualRatePercent >= 0) { "annualRatePercent must be >= 0" }
        require(years > 0) { "years must be > 0" }
        require(timesPerYear >= 1) { "timesPerYear must be >= 1" }

        val rate = annualRatePercent / PERCENT_BASE
        val maturity = (principal * (1.0 + rate / timesPerYear).pow(timesPerYear * years)).requireFinite()
        return InvestmentResult(
            maturity = maturity,
            invested = principal,
            gains = maturity - principal,
        )
    }

    /**
     * Compound annual growth rate that turns [startValue] into [endValue] in [years].
     * An end value below the start gives a negative rate; an end value of 0 gives -100 %.
     *
     * @throws IllegalArgumentException when [startValue] or [years] is not positive or
     *   [endValue] is negative.
     * @throws ResultTooLargeException when the rate overflows a Double.
     */
    fun cagr(
        startValue: Double,
        endValue: Double,
        years: Double,
    ): CagrResult {
        require(startValue > 0) { "startValue must be > 0" }
        require(endValue >= 0) { "endValue must be >= 0" }
        require(years > 0) { "years must be > 0" }

        val growth = (endValue / startValue).pow(1.0 / years) - 1.0
        return CagrResult(
            cagrPercent = (growth * PERCENT_BASE).requireFinite(),
            absoluteReturnPercent = PercentCalculator.percentChange(startValue, endValue).requireFinite(),
            gain = endValue - startValue,
        )
    }
}

/**
 * Result of [InvestmentCalculator], in the investment's currency.
 *
 * @property maturity Value at the end of the term.
 * @property invested Total paid in.
 * @property gains [maturity] minus [invested].
 */
data class InvestmentResult(
    val maturity: Double,
    val invested: Double,
    val gains: Double,
)

/**
 * Result of [InvestmentCalculator.cagr].
 *
 * @property cagrPercent Growth per year, in percent.
 * @property absoluteReturnPercent Total growth over the whole period, in percent.
 * @property gain End value minus start value; negative for a loss.
 */
data class CagrResult(
    val cagrPercent: Double,
    val absoluteReturnPercent: Double,
    val gain: Double,
)

private const val MONTHS_PER_YEAR = 12.0
private const val PERCENT_BASE = 100.0
