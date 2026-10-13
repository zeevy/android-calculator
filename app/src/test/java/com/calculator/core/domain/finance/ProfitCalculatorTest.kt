package com.calculator.core.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Known answers for the profit, markup and margin math in [ProfitCalculator]. */
class ProfitCalculatorTest {
    @Test
    fun `cost 800 and selling 1000 give profit 200, markup 25 and margin 20`() {
        val result = ProfitCalculator.fromPrices(cost = 800.0, selling = 1000.0)

        assertEquals(200.0, result.profit, 1e-9)
        assertEquals(25.0, result.markupPercent!!, 1e-9)
        assertEquals(20.0, result.marginPercent!!, 1e-9)
    }

    @Test
    fun `cost 800 with a markup of 25 percent sells at 1000`() {
        val result = ProfitCalculator.fromMarkup(cost = 800.0, markupPercent = 25.0)

        assertEquals(1000.0, result.selling, 1e-9)
        assertEquals(200.0, result.profit, 1e-9)
        assertEquals(20.0, result.marginPercent!!, 1e-9)
    }

    @Test
    fun `cost 800 with a margin of 20 percent sells at 1000`() {
        val result = ProfitCalculator.fromMargin(cost = 800.0, marginPercent = 20.0)

        assertEquals(1000.0, result.selling, 1e-9)
        assertEquals(200.0, result.profit, 1e-9)
        assertEquals(25.0, result.markupPercent!!, 1e-9)
    }

    @Test
    fun `selling below cost gives a negative profit, markup and margin`() {
        val result = ProfitCalculator.fromPrices(cost = 1000.0, selling = 800.0)

        assertEquals(-200.0, result.profit, 1e-9)
        assertEquals(-20.0, result.markupPercent!!, 1e-9)
        assertEquals(-25.0, result.marginPercent!!, 1e-9)
    }

    @Test
    fun `selling at cost gives zero profit, markup and margin`() {
        val result = ProfitCalculator.fromPrices(cost = 500.0, selling = 500.0)

        assertEquals(0.0, result.profit, 1e-9)
        assertEquals(0.0, result.markupPercent!!, 1e-9)
        assertEquals(0.0, result.marginPercent!!, 1e-9)
    }

    @Test
    fun `cost 0 has no markup`() {
        val result = ProfitCalculator.fromPrices(cost = 0.0, selling = 100.0)

        assertNull(result.markupPercent)
        assertEquals(100.0, result.marginPercent!!, 1e-9)
    }

    @Test
    fun `selling 0 has no margin`() {
        val result = ProfitCalculator.fromPrices(cost = 100.0, selling = 0.0)

        assertNull(result.marginPercent)
        assertEquals(-100.0, result.markupPercent!!, 1e-9)
    }

    @Test
    fun `a margin of 100 or more throws`() {
        assertThrows<IllegalArgumentException> { ProfitCalculator.fromMargin(cost = 800.0, marginPercent = 100.0) }
        assertThrows<IllegalArgumentException> { ProfitCalculator.fromMargin(cost = 800.0, marginPercent = 150.0) }
    }

    @Test
    fun `negative values throw`() {
        assertThrows<IllegalArgumentException> { ProfitCalculator.fromPrices(cost = -1.0, selling = 10.0) }
        assertThrows<IllegalArgumentException> { ProfitCalculator.fromPrices(cost = 1.0, selling = -10.0) }
        assertThrows<IllegalArgumentException> { ProfitCalculator.fromMarkup(cost = 1.0, markupPercent = -5.0) }
        assertThrows<IllegalArgumentException> { ProfitCalculator.fromMargin(cost = 1.0, marginPercent = -5.0) }
    }

    @Test
    fun `a selling price that overflows throws ResultTooLargeException`() {
        assertThrows<ResultTooLargeException> { ProfitCalculator.fromMarkup(cost = 1e308, markupPercent = 100.0) }
    }
}
