package com.calculator.core.domain.finance

/**
 * Indian GST splits.
 *
 * For an **intra-state** sale the total tax is split equally between
 * CGST (Central GST) and SGST (State GST). For **inter-state** sales
 * the same total goes entirely to IGST (Integrated GST).
 *
 * The calculator supports two directions:
 *  - [forward]: net amount + rate → tax breakdown + gross.
 *  - [reverse]: gross amount + rate → net + tax breakdown.
 *
 * The rate is the standard slab percentage (5/12/18/28) but any
 * non-negative value is accepted - a settings UI can expose a custom-
 * rate field.
 */
object GstCalculator {
    /**
     * Tax on a [net] amount at [ratePercent] (18.0 for 18%).
     *
     * @param intraState True within one state (CGST + SGST), false between states (IGST).
     * @throws IllegalArgumentException when [net] or [ratePercent] is negative.
     */
    fun forward(net: Double, ratePercent: Double, intraState: Boolean): GstResult {
        require(net >= 0) { "net must be >= 0" }
        require(ratePercent >= 0) { "rate must be >= 0" }
        val tax = net * ratePercent / 100.0
        return splitResult(net, gross = net + tax, tax, intraState)
    }

    /**
     * Net amount and tax inside a [gross] amount that includes GST at [ratePercent].
     *
     * @param intraState True within one state (CGST + SGST), false between states (IGST).
     * @throws IllegalArgumentException when [gross] or [ratePercent] is negative.
     */
    fun reverse(gross: Double, ratePercent: Double, intraState: Boolean): GstResult {
        require(gross >= 0) { "gross must be >= 0" }
        require(ratePercent >= 0) { "rate must be >= 0" }
        val net = gross / (1.0 + ratePercent / 100.0)
        val tax = gross - net
        return splitResult(net, gross, tax, intraState)
    }

    private fun splitResult(
        net: Double,
        gross: Double,
        tax: Double,
        intraState: Boolean,
    ): GstResult =
        if (intraState) {
            GstResult(
                net = net,
                gross = gross,
                cgst = tax / 2.0,
                sgst = tax / 2.0,
                igst = 0.0,
            )
        } else {
            GstResult(net = net, gross = gross, cgst = 0.0, sgst = 0.0, igst = tax)
        }
}

/**
 * Result of [GstCalculator], in the bill's currency.
 *
 * @property net Amount before tax.
 * @property gross Amount including tax.
 * @property cgst Central GST; half the tax within a state, else 0.
 * @property sgst State GST; half the tax within a state, else 0.
 * @property igst Integrated GST; all the tax between states, else 0.
 */
data class GstResult(
    val net: Double,
    val gross: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
)
