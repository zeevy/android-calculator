package com.calculator.core.domain.finance

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
 * The math is pure `Double`; rounding to the user's preferred precision
 * is the display layer's responsibility.
 */
object InvestmentCalculator {
    fun sip(
        monthlyAmount: Double,
        annualRatePercent: Double,
        years: Double,
    ): InvestmentResult {
        require(monthlyAmount >= 0) { "monthlyAmount must be >= 0" }
        require(annualRatePercent >= 0) { "annualRatePercent must be >= 0" }
        require(years > 0) { "years must be > 0" }

        val months = years * 12.0
        val monthlyRate = annualRatePercent / 100.0 / 12.0
        val maturity =
            if (monthlyRate == 0.0) {
                monthlyAmount * months
            } else {
                monthlyAmount *
                    ((1.0 + monthlyRate).pow(months) - 1.0) / monthlyRate *
                    (1.0 + monthlyRate)
            }
        val invested = monthlyAmount * months
        return InvestmentResult(maturity = maturity, invested = invested, gains = maturity - invested)
    }

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

        val rate = annualRatePercent / 100.0
        val maturity = principal * (1.0 + rate / timesPerYear).pow(timesPerYear * years)
        return InvestmentResult(
            maturity = maturity,
            invested = principal,
            gains = maturity - principal,
        )
    }
}

data class InvestmentResult(
    val maturity: Double,
    val invested: Double,
    val gains: Double,
)
