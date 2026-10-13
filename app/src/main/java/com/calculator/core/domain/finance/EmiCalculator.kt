package com.calculator.core.domain.finance

import kotlin.math.pow

/**
 * Equated Monthly Installment (EMI) for a fixed-rate amortising loan.
 *
 *     EMI = P × r × (1+r)^n / ((1+r)^n − 1)
 *
 * where `P` is principal, `r` is the **monthly** interest rate, and
 * `n` is the number of monthly payments. Zero-interest loans short-
 * circuit to `P / n` (the formula above divides by zero in that case).
 *
 * All amounts are plain `Double` - financial precision past two
 * decimals is irrelevant for an EMI estimator; the UI rounds to the
 * locale's currency precision at the boundary.
 *
 * The calculator is **strictly an estimator** - the user-facing copy
 * must not imply this app is a lender or that the user "qualifies"
 * for anything (Play personal-loans policy).
 */
object EmiCalculator {
    /**
     * @param principal Loan amount in the user's currency.
     * @param annualRatePercent Annual interest rate as a percentage
     *   (e.g. `10.5` for 10.5%, not `0.105`).
     * @param months Tenure in months.
     * @throws IllegalArgumentException for a non-positive principal or tenure, or a negative rate.
     * @throws ResultTooLargeException when the EMI overflows a Double.
     */
    fun emi(
        principal: Double,
        annualRatePercent: Double,
        months: Int,
    ): EmiResult {
        require(principal > 0) { "principal must be > 0" }
        require(months > 0) { "months must be > 0" }
        require(annualRatePercent >= 0) { "rate must be >= 0" }
        val emi =
            if (annualRatePercent == 0.0) {
                principal / months
            } else {
                val r = monthlyRate(annualRatePercent)
                val pow = (1.0 + r).pow(months)
                (principal * r * pow / (pow - 1.0)).requireFinite()
            }
        return EmiResult(emi = emi, totalInterest = emi * months - principal, totalPayment = emi * months)
    }

    /**
     * Month-by-month repayment schedule for the same loan as [emi], one
     * row per month (index 1 is the first payment). Builds a list of
     * [months] rows, so call it only when the schedule is shown.
     *
     * @throws IllegalArgumentException for the same inputs [emi] rejects.
     * @throws ResultTooLargeException when the EMI overflows a Double.
     */
    fun schedule(
        principal: Double,
        annualRatePercent: Double,
        months: Int,
    ): List<AmortisationRow> =
        amortisation(principal, monthlyRate(annualRatePercent), months, emi(principal, annualRatePercent, months).emi)

    private fun monthlyRate(annualRatePercent: Double) = annualRatePercent / 100.0 / MONTHS_PER_YEAR

    private fun amortisation(
        principal: Double,
        monthlyRate: Double,
        months: Int,
        emi: Double,
    ): List<AmortisationRow> {
        var balance = principal
        return List(months) { idx ->
            val interest = balance * monthlyRate
            val principalPaid =
                if (idx == months - 1) {
                    // Last row: pay off whatever's left so the balance
                    // lands cleanly on zero despite Double rounding.
                    balance
                } else {
                    emi - interest
                }
            balance -= principalPaid
            AmortisationRow(
                monthIndex = idx + 1,
                principalPaid = principalPaid,
                interestPaid = interest,
                balanceAfter = if (idx == months - 1) 0.0 else balance,
            )
        }
    }

    private const val MONTHS_PER_YEAR = 12.0
}

/**
 * Result of [EmiCalculator.emi], in the loan's currency.
 *
 * @property emi Monthly payment.
 * @property totalInterest Interest paid over the whole tenure.
 * @property totalPayment Principal plus interest.
 */
data class EmiResult(
    val emi: Double,
    val totalInterest: Double,
    val totalPayment: Double,
)

/**
 * One month of [EmiCalculator.schedule].
 *
 * @property monthIndex Payment number, starting at 1.
 * @property principalPaid Part of the payment that reduces the balance.
 * @property interestPaid Part of the payment that is interest.
 * @property balanceAfter Balance left after this payment; 0 after the last.
 */
data class AmortisationRow(
    val monthIndex: Int,
    val principalPaid: Double,
    val interestPaid: Double,
    val balanceAfter: Double,
)
