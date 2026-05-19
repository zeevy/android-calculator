package com.calculator.core.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DiscountCalculatorTest {
    @Test
    fun `forward MRP 2000 20pct off gives final 1600`() {
        val r = DiscountCalculator.forward(mrp = 2_000.0, percentOff = 20.0)
        assertEquals(1_600.0, r.finalPrice, 1e-9)
        assertEquals(400.0, r.savings, 1e-9)
    }

    @Test
    fun `reverse MRP 2000 final 1500 yields 25pct`() {
        val r = DiscountCalculator.reverse(mrp = 2_000.0, finalPrice = 1_500.0)
        assertEquals(25.0, r.percentOff, 1e-9)
        assertEquals(500.0, r.savings, 1e-9)
    }

    @Test
    fun `forward 100pct off prices the item at zero`() {
        val r = DiscountCalculator.forward(mrp = 999.0, percentOff = 100.0)
        assertEquals(0.0, r.finalPrice, 1e-9)
    }
}
