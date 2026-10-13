package com.calculator.core.domain.finance

import kotlin.math.pow

/**
 * Maturity math for the three common Indian deposits, done the way banks,
 * India Post and the PPF scheme rules do it.
 *
 *  - **FD (fixed deposit)**: interest is compounded every full period
 *    ([InvestmentCalculator.lumpSum]). The months after the last full
 *    period earn simple interest on the compounded amount. A deposit
 *    below 6 months, or one shorter than one period, earns only simple
 *    interest:
 *
 *        A = P × (1 + r/n)^(whole periods) × (1 + r × leftover months / 12)
 *
 *  - **RD (recurring deposit)**: one deposit at the start of every month,
 *    compounded every quarter. Each deposit earns interest for the months
 *    it stays in:
 *
 *        M = Σ (m = 1..N) R × (1 + r/4)^(m/3)
 *
 *  - **PPF (Public Provident Fund)**: one deposit at the start of every
 *    financial year (by 5 April, so it earns the full year), compounded
 *    yearly for [PPF_YEARS] years:
 *
 *        M = P × [((1 + r)^15 - 1) / r] × (1 + r)
 *
 * The math is pure `Double`; rounding to paise is the display layer's job.
 */
object DepositCalculator {
    /** Years of a PPF account: the scheme matures after 15 full financial years. */
    const val PPF_YEARS = 15

    /** Lowest PPF deposit allowed in one financial year. */
    const val PPF_MIN_YEARLY_DEPOSIT = 500.0

    /** Highest PPF deposit allowed in one financial year. */
    const val PPF_MAX_YEARLY_DEPOSIT = 150_000.0

    /**
     * Value of a fixed deposit of [principal] for [months] at [annualRatePercent],
     * compounded [timesPerYear] times a year.
     *
     * @param timesPerYear 12, 4, 2 or 1 (monthly, quarterly, half-yearly, yearly).
     * @throws IllegalArgumentException when an amount or rate is negative, [months] is
     *   not positive or [timesPerYear] does not divide a year into whole months.
     * @throws ResultTooLargeException when the value overflows a Double.
     */
    fun fixedDeposit(
        principal: Double,
        annualRatePercent: Double,
        months: Int,
        timesPerYear: Int,
    ): DepositResult {
        require(principal >= 0) { "principal must be >= 0" }
        require(annualRatePercent >= 0) { "annualRatePercent must be >= 0" }
        require(months > 0) { "months must be > 0" }
        require(timesPerYear in 1..MONTHS_PER_YEAR && MONTHS_PER_YEAR % timesPerYear == 0) {
            "timesPerYear must divide 12"
        }

        val rate = annualRatePercent / PERCENT_BASE
        val monthsPerPeriod = MONTHS_PER_YEAR / timesPerYear
        val wholePeriods = months / monthsPerPeriod
        val maturity =
            if (months < SIMPLE_INTEREST_BELOW_MONTHS || wholePeriods == 0) {
                principal * (1.0 + rate * months / MONTHS_PER_YEAR)
            } else {
                val compounded =
                    InvestmentCalculator
                        .lumpSum(principal, annualRatePercent, wholePeriods.toDouble() / timesPerYear, timesPerYear)
                        .maturity
                val leftoverMonths = months % monthsPerPeriod
                compounded * (1.0 + rate * leftoverMonths / MONTHS_PER_YEAR)
            }.requireFinite()
        return DepositResult(maturity = maturity, deposited = principal, interest = maturity - principal)
    }

    /**
     * Value of a recurring deposit of [monthlyDeposit] for [months] at [annualRatePercent],
     * compounded every quarter.
     *
     * @throws IllegalArgumentException when an amount or rate is negative or [months] is not positive.
     * @throws ResultTooLargeException when the value overflows a Double.
     */
    fun recurringDeposit(
        monthlyDeposit: Double,
        annualRatePercent: Double,
        months: Int,
    ): DepositResult {
        require(monthlyDeposit >= 0) { "monthlyDeposit must be >= 0" }
        require(annualRatePercent >= 0) { "annualRatePercent must be >= 0" }
        require(months > 0) { "months must be > 0" }

        val quarterlyGrowth = 1.0 + annualRatePercent / PERCENT_BASE / QUARTERS_PER_YEAR
        val maturity =
            (1..months)
                .sumOf { monthsIn -> monthlyDeposit * quarterlyGrowth.pow(monthsIn.toDouble() / MONTHS_PER_QUARTER) }
                .requireFinite()
        val deposited = monthlyDeposit * months
        return DepositResult(maturity = maturity, deposited = deposited, interest = maturity - deposited)
    }

    /**
     * Value of a PPF account with [yearlyDeposit] paid in by 5 April of each of the
     * [PPF_YEARS] years at [annualRatePercent], with the balance for every year.
     *
     * @throws IllegalArgumentException when [yearlyDeposit] is outside the scheme's
     *   limits ([PPF_MIN_YEARLY_DEPOSIT] to [PPF_MAX_YEARLY_DEPOSIT]) or the rate is negative.
     * @throws ResultTooLargeException when the value overflows a Double.
     */
    fun ppf(
        yearlyDeposit: Double,
        annualRatePercent: Double,
    ): PpfResult {
        require(yearlyDeposit in PPF_MIN_YEARLY_DEPOSIT..PPF_MAX_YEARLY_DEPOSIT) {
            "yearlyDeposit must be from $PPF_MIN_YEARLY_DEPOSIT to $PPF_MAX_YEARLY_DEPOSIT"
        }
        require(annualRatePercent >= 0) { "annualRatePercent must be >= 0" }

        val rate = annualRatePercent / PERCENT_BASE
        var balance = 0.0
        val years =
            (1..PPF_YEARS).map { year ->
                val interest = (balance + yearlyDeposit) * rate
                balance = (balance + yearlyDeposit + interest).requireFinite()
                PpfYear(year = year, deposit = yearlyDeposit, interest = interest, closing = balance)
            }
        val deposited = yearlyDeposit * PPF_YEARS
        return PpfResult(maturity = balance, deposited = deposited, interest = balance - deposited, years = years)
    }
}

/**
 * Result of an FD or RD in [DepositCalculator].
 *
 * @property maturity Value at the end of the term.
 * @property deposited Total paid in.
 * @property interest [maturity] minus [deposited].
 */
data class DepositResult(
    val maturity: Double,
    val deposited: Double,
    val interest: Double,
)

/**
 * Result of [DepositCalculator.ppf].
 *
 * @property maturity Balance at the end of the last year.
 * @property deposited Total paid in.
 * @property interest [maturity] minus [deposited].
 * @property years The balance for each year, first year first.
 */
data class PpfResult(
    val maturity: Double,
    val deposited: Double,
    val interest: Double,
    val years: List<PpfYear>,
)

/**
 * One year of a PPF account.
 *
 * @property year 1 for the first year.
 * @property deposit Paid in at the start of the year.
 * @property interest Interest credited at the end of the year.
 * @property closing Balance at the end of the year.
 */
data class PpfYear(
    val year: Int,
    val deposit: Double,
    val interest: Double,
    val closing: Double,
)

private const val MONTHS_PER_YEAR = 12
private const val MONTHS_PER_QUARTER = 3.0
private const val QUARTERS_PER_YEAR = 4.0
private const val PERCENT_BASE = 100.0

// SBI and HDFC pay simple interest on deposits below 6 months.
private const val SIMPLE_INTEREST_BELOW_MONTHS = 6
