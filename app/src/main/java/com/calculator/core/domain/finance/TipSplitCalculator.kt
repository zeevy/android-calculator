package com.calculator.core.domain.finance

import kotlin.math.ceil

/**
 * Tip + bill-split arithmetic.
 *
 * Given a bill, a tip percentage, and the number of people splitting
 * it, produces the tip amount, the grand total, and the per-person
 * share. Optional [roundUpPerPerson] bumps each share up to the next
 * whole currency unit so people can pay without small change (any
 * positive remainder folds back into the tip).
 *
 * The math is plain `Double` - bills round to two decimal places at the
 * display layer; the engine carries full precision.
 */
object TipSplitCalculator {
    fun compute(
        bill: Double,
        tipPercent: Double,
        people: Int,
        roundUpPerPerson: Boolean = false,
    ): TipSplitResult {
        require(bill >= 0) { "bill must be >= 0" }
        require(tipPercent >= 0) { "tipPercent must be >= 0" }
        require(people >= 1) { "people must be >= 1" }

        val rawTip = bill * tipPercent / 100.0
        val rawTotal = bill + rawTip
        val rawPerPerson = rawTotal / people

        val perPerson = if (roundUpPerPerson) ceil(rawPerPerson) else rawPerPerson
        val total = perPerson * people
        // Adjust tip to the new total when rounding bumps each share.
        val tip = total - bill
        return TipSplitResult(
            tip = tip,
            total = total,
            perPerson = perPerson,
        )
    }
}

data class TipSplitResult(
    val tip: Double,
    val total: Double,
    val perPerson: Double,
)
