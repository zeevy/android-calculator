package com.calculator.core.domain.finance

import kotlin.math.pow
import kotlin.math.round

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
     */
    fun emi(principal: Double, annualRatePercent: Double, months: Int): EmiResult {
        require(principal > 0) { "principal must be > 0" }
        require(months > 0) { "months must be > 0" }
        require(annualRatePercent >= 0) { "rate must be >= 0" }
        if (annualRatePercent == 0.0) {
            val flat = principal / months
            return EmiResult(
                emi = flat,
                totalInterest = 0.0,
                totalPayment = principal,
                amortisation = flatAmortisation(principal, months, flat),
            )
        }
        val r = annualRatePercent / 100.0 / MONTHS_PER_YEAR
        val pow = (1.0 + r).pow(months)
        val emi = principal * r * pow / (pow - 1.0)
        return EmiResult(
            emi = emi,
            totalInterest = emi * months - principal,
            totalPayment = emi * months,
            amortisation = amortisation(principal, r, months, emi),
        )
    }

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

    private fun flatAmortisation(
        principal: Double,
        months: Int,
        flat: Double,
    ): List<AmortisationRow> {
        var balance = principal
        return List(months) { idx ->
            val pay = if (idx == months - 1) balance else flat
            balance -= pay
            AmortisationRow(
                monthIndex = idx + 1,
                principalPaid = pay,
                interestPaid = 0.0,
                balanceAfter = if (idx == months - 1) 0.0 else balance,
            )
        }
    }

    private const val MONTHS_PER_YEAR = 12.0
}

data class EmiResult(
    val emi: Double,
    val totalInterest: Double,
    val totalPayment: Double,
    val amortisation: List<AmortisationRow>,
)

data class AmortisationRow(
    val monthIndex: Int,
    val principalPaid: Double,
    val interestPaid: Double,
    val balanceAfter: Double,
)

/** Round to two decimals; visible in tests for stable assertions. */
internal fun Double.round2(): Double = round(this * 100) / 100
