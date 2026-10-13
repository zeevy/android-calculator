package com.calculator.core.domain.finance

/**
 * Discount math, both directions.
 *
 * Forward: starting price + percent off → final price + savings.
 * Reverse: starting price + final price → effective percent.
 */
object DiscountCalculator {
    /**
     * Savings and final price for [percentOff] off an item priced [mrp].
     *
     * @param percentOff e.g. `20.0` for 20% off.
     * @throws IllegalArgumentException when [mrp] is negative or [percentOff] is outside 0 to 100.
     */
    fun forward(mrp: Double, percentOff: Double): DiscountResult {
        require(mrp >= 0) { "mrp must be >= 0" }
        require(percentOff in 0.0..100.0) { "percent must be in 0..100" }
        val savings = mrp * percentOff / 100.0
        return DiscountResult(
            mrp = mrp,
            percentOff = percentOff,
            savings = savings,
            finalPrice = mrp - savings,
        )
    }

    /**
     * Returns the effective percent off when paying [finalPrice] for an item with [mrp].
     *
     * @throws IllegalArgumentException when [mrp] is not positive or [finalPrice] is outside 0 to [mrp].
     */
    fun reverse(mrp: Double, finalPrice: Double): DiscountResult {
        require(mrp > 0) { "mrp must be > 0" }
        require(finalPrice in 0.0..mrp) { "final must be in 0..mrp" }
        val savings = mrp - finalPrice
        val percent = savings / mrp * 100.0
        return DiscountResult(
            mrp = mrp,
            percentOff = percent,
            savings = savings,
            finalPrice = finalPrice,
        )
    }
}

/**
 * Result of [DiscountCalculator], in the item's currency.
 *
 * @property mrp Price before the discount.
 * @property percentOff Discount in percent (20.0 for 20%).
 * @property savings Amount saved.
 * @property finalPrice Price paid.
 */
data class DiscountResult(
    val mrp: Double,
    val percentOff: Double,
    val savings: Double,
    val finalPrice: Double,
)
