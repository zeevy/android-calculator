package com.calculator.core.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Known answers for the SIP and lump sum math in [InvestmentCalculator]. */
class InvestmentCalculatorTest {
    @Test
    fun `sip of 5000 a month at 12 percent for 10 years`() {
        val result = InvestmentCalculator.sip(monthlyAmount = 5000.0, annualRatePercent = 12.0, years = 10.0)

        // 5000 x ((1.01^120 - 1) / 0.01) x 1.01, worked out with 40 digit decimals.
        assertEquals(1_161_695.38, result.maturity, 0.5)
        assertEquals(600_000.0, result.invested, 1e-9)
        assertEquals(561_695.38, result.gains, 0.5)
    }

    @Test
    fun `lump sum of 100000 at 10 percent for 5 years compounded yearly`() {
        val result = InvestmentCalculator.lumpSum(principal = 100_000.0, annualRatePercent = 10.0, years = 5.0)

        assertEquals(161_051.0, result.maturity, 1e-6)
        assertEquals(100_000.0, result.invested, 1e-9)
        assertEquals(61_051.0, result.gains, 1e-6)
    }

    @Test
    fun `lump sum of 100000 at 10 percent for 5 years compounded every quarter`() {
        val result =
            InvestmentCalculator.lumpSum(principal = 100_000.0, annualRatePercent = 10.0, years = 5.0, timesPerYear = 4)

        // 100000 x 1.025^20
        assertEquals(163_861.64, result.maturity, 0.01)
    }

    @Test
    fun `sip at zero rate gives back only what was put in`() {
        val result = InvestmentCalculator.sip(monthlyAmount = 2000.0, annualRatePercent = 0.0, years = 3.0)

        assertEquals(72_000.0, result.maturity, 1e-9)
        assertEquals(result.invested, result.maturity, 1e-9)
        assertEquals(0.0, result.gains, 1e-9)
    }

    @Test
    fun `lump sum at zero rate gives back only what was put in`() {
        val result = InvestmentCalculator.lumpSum(principal = 50_000.0, annualRatePercent = 0.0, years = 7.0)

        assertEquals(50_000.0, result.maturity, 1e-9)
        assertEquals(result.invested, result.maturity, 1e-9)
        assertEquals(0.0, result.gains, 1e-9)
    }

    @Test
    fun `invested plus gains is the maturity`() {
        val results =
            listOf(
                InvestmentCalculator.sip(monthlyAmount = 1500.0, annualRatePercent = 8.5, years = 7.5),
                InvestmentCalculator.lumpSum(principal = 25_000.0, annualRatePercent = 6.75, years = 12.0, timesPerYear = 12),
            )

        results.forEach { assertEquals(it.maturity, it.invested + it.gains, 1e-6) }
    }
}
