package com.calculator.core.domain.finance

/**
 * Profit, markup and margin between a cost price (CP) and a selling price (SP).
 *
 *        profit   = SP - CP
 *        markup % = profit / CP × 100      (profit on cost)
 *        margin % = profit / SP × 100      (profit on selling price)
 *
 * The selling price can be given directly, or worked out from a markup or a margin:
 *
 *        SP = CP × (1 + markup / 100)
 *        SP = CP / (1 - margin / 100)
 *
 * A loss gives a negative profit, markup and margin. The math is pure `Double`;
 * rounding for display is the caller's job.
 */
object ProfitCalculator {
    /**
     * Profit when [cost] and [selling] are both known.
     *
     * @throws IllegalArgumentException when a price is negative.
     * @throws ResultTooLargeException when a percentage overflows a Double.
     */
    fun fromPrices(
        cost: Double,
        selling: Double,
    ): ProfitResult {
        require(cost >= 0) { "cost must be >= 0" }
        require(selling >= 0) { "selling must be >= 0" }
        return result(cost, selling)
    }

    /**
     * Selling price and profit for [cost] plus a markup of [markupPercent] on cost.
     *
     * @throws IllegalArgumentException when [cost] or [markupPercent] is negative.
     * @throws ResultTooLargeException when the selling price overflows a Double.
     */
    fun fromMarkup(
        cost: Double,
        markupPercent: Double,
    ): ProfitResult {
        require(cost >= 0) { "cost must be >= 0" }
        require(markupPercent >= 0) { "markupPercent must be >= 0" }
        return result(cost, (cost * (1.0 + markupPercent / PERCENT_BASE)).requireFinite())
    }

    /**
     * Selling price and profit for [cost] when [marginPercent] of the selling price is profit.
     *
     * @throws IllegalArgumentException when [cost] is negative or [marginPercent] is not in 0 until 100.
     * @throws ResultTooLargeException when the selling price overflows a Double.
     */
    fun fromMargin(
        cost: Double,
        marginPercent: Double,
    ): ProfitResult {
        require(cost >= 0) { "cost must be >= 0" }
        require(marginPercent >= 0 && marginPercent < PERCENT_BASE) { "marginPercent must be in 0 until 100" }
        return result(cost, (cost / (1.0 - marginPercent / PERCENT_BASE)).requireFinite())
    }

    private fun result(
        cost: Double,
        selling: Double,
    ): ProfitResult {
        val profit = selling - cost
        return ProfitResult(
            cost = cost,
            selling = selling,
            profit = profit,
            markupPercent = if (cost == 0.0) null else (profit / cost * PERCENT_BASE).requireFinite(),
            marginPercent = if (selling == 0.0) null else (profit / selling * PERCENT_BASE).requireFinite(),
        )
    }
}

/**
 * Result of [ProfitCalculator], in the prices' currency.
 *
 * @property cost Cost price.
 * @property selling Selling price.
 * @property profit [selling] minus [cost]; negative for a loss.
 * @property markupPercent Profit as a percentage of [cost]; null when [cost] is 0.
 * @property marginPercent Profit as a percentage of [selling]; null when [selling] is 0.
 */
data class ProfitResult(
    val cost: Double,
    val selling: Double,
    val profit: Double,
    val markupPercent: Double?,
    val marginPercent: Double?,
)

private const val PERCENT_BASE = 100.0
