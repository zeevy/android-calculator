package com.calculator.core.domain.math

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** The three percent questions answered by [PercentCalculator]. */
class PercentCalculatorTest {
    @Test
    fun `percent of a value`() {
        assertEquals(50.0, PercentCalculator.percentOf(20.0, 250.0), 1e-9)
        assertEquals(0.0, PercentCalculator.percentOf(0.0, 250.0), 1e-9)
        assertEquals(375.0, PercentCalculator.percentOf(150.0, 250.0), 1e-9)
    }

    @Test
    fun `what percent one value is of another`() {
        assertEquals(25.0, PercentCalculator.whatPercent(30.0, 120.0), 1e-9)
        assertEquals(200.0, PercentCalculator.whatPercent(240.0, 120.0), 1e-9)
    }

    @Test
    fun `percent change going up is positive`() {
        assertEquals(25.0, PercentCalculator.percentChange(80.0, 100.0), 1e-9)
    }

    @Test
    fun `percent change going down is negative`() {
        assertEquals(-20.0, PercentCalculator.percentChange(100.0, 80.0), 1e-9)
        assertEquals(-100.0, PercentCalculator.percentChange(50.0, 0.0), 1e-9)
    }

    @Test
    fun `what percent of zero is rejected`() {
        assertThrows<IllegalArgumentException> { PercentCalculator.whatPercent(5.0, 0.0) }
    }

    @Test
    fun `percent change from zero is rejected`() {
        assertThrows<IllegalArgumentException> { PercentCalculator.percentChange(0.0, 10.0) }
    }
}
