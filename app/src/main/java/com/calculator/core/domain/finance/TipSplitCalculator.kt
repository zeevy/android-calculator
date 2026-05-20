package com.calculator.core.domain.finance

import kotlin.math.ceil

/**
 * Tip + bill-split arithmetic.
 *
 * Given a bill, a fixed [tipAmount] in the same currency, and the
 * number of people splitting it, produces the grand total and each
 * person's share. Optional [roundUpPerPerson] bumps each share up to
 * the next whole currency unit so people can pay without small change
 * (any positive remainder folds back into the tip).
 *
 * The math is plain `Double` - bills round to two decimal places at the
 * display layer; the engine carries full precision.
 */
object TipSplitCalculator {
    fun compute(
        bill: Double,
        tipAmount: Double,
        people: Int,
        roundUpPerPerson: Boolean = false,
    ): TipSplitResult {
        require(bill >= 0) { "bill must be >= 0" }
        require(tipAmount >= 0) { "tipAmount must be >= 0" }
        require(people >= 1) { "people must be >= 1" }

        val rawTotal = bill + tipAmount
        val rawPerPerson = rawTotal / people

        val perPerson = if (roundUpPerPerson) ceil(rawPerPerson) else rawPerPerson
        val total = perPerson * people
        // Adjust tip upward when rounding bumps each share so the
        // numbers stay internally consistent (bill + tip == people *
        // perPerson). The user typed `tipAmount` as a floor; the tip
        // we report can be slightly larger if round-up was on.
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
