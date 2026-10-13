package com.calculator.core.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.pow

/** Known answers for the FD, RD and PPF math in [DepositCalculator]. */
class DepositCalculatorTest {
    @Test
    fun `fd of 100000 at 7 percent for 5 years compounded every quarter`() {
        val result = DepositCalculator.fixedDeposit(100_000.0, 7.0, months = 60, timesPerYear = 4)

        // 100000 x 1.0175^20
        assertEquals(141_477.82, result.maturity, 0.005)
        assertEquals(100_000.0, result.deposited, 1e-9)
        assertEquals(41_477.82, result.interest, 0.005)
    }

    @Test
    fun `fd compounds full quarters and pays simple interest for the leftover months`() {
        val result = DepositCalculator.fixedDeposit(100_000.0, 7.0, months = 14, timesPerYear = 4)

        // 100000 x 1.0175^4 x (1 + 0.07 x 2 / 12)
        assertEquals(108_436.41, result.maturity, 0.005)
    }

    @Test
    fun `fd below 6 months earns simple interest`() {
        val result = DepositCalculator.fixedDeposit(100_000.0, 7.0, months = 4, timesPerYear = 4)

        assertEquals(102_333.33, result.maturity, 0.005)
    }

    @Test
    fun `fd shorter than one yearly period earns simple interest`() {
        val result = DepositCalculator.fixedDeposit(100_000.0, 6.0, months = 8, timesPerYear = 1)

        assertEquals(104_000.0, result.maturity, 1e-6)
    }

    @Test
    fun `fd compounded monthly for one year`() {
        val result = DepositCalculator.fixedDeposit(100_000.0, 7.0, months = 12, timesPerYear = 12)

        assertEquals(100_000.0 * (1 + 0.07 / 12).pow(12), result.maturity, 1e-6)
    }

    @Test
    fun `fd at zero rate gives back the deposit`() {
        assertEquals(50_000.0, DepositCalculator.fixedDeposit(50_000.0, 0.0, 24, 4).maturity, 1e-9)
    }

    @Test
    fun `fd needs a tenure and a compounding that fits a year`() {
        assertThrows<IllegalArgumentException> { DepositCalculator.fixedDeposit(1_000.0, 7.0, 0, 4) }
        assertThrows<IllegalArgumentException> { DepositCalculator.fixedDeposit(1_000.0, 7.0, 12, 5) }
    }

    @Test
    fun `rd of 1000 a month at 7 percent for 12 months`() {
        val result = DepositCalculator.recurringDeposit(1_000.0, 7.0, months = 12)

        assertEquals(12_462.13, result.maturity, 0.005)
        assertEquals(12_000.0, result.deposited, 1e-9)
        assertEquals(462.13, result.interest, 0.005)
    }

    @Test
    fun `rd matches the India Post table for 100 a month for 5 years at 6_7 percent`() {
        val result = DepositCalculator.recurringDeposit(100.0, 6.7, months = 60)

        assertEquals(7_136.58, result.maturity, 0.005)
    }

    @Test
    fun `rd matches the Groww example of 5000 a month for 12 months at 8_25 percent`() {
        assertEquals(62_730.85, DepositCalculator.recurringDeposit(5_000.0, 8.25, 12).maturity, 0.005)
    }

    @Test
    fun `rd of one month earns a third of a quarter`() {
        val result = DepositCalculator.recurringDeposit(1_000.0, 8.0, months = 1)

        assertEquals(1_000.0 * 1.02.pow(1.0 / 3), result.maturity, 1e-9)
    }

    @Test
    fun `rd at zero rate gives back the deposits`() {
        assertEquals(24_000.0, DepositCalculator.recurringDeposit(2_000.0, 0.0, 12).maturity, 1e-9)
    }

    @Test
    fun `ppf of 150000 a year at 7_1 percent for 15 years`() {
        val result = DepositCalculator.ppf(150_000.0, 7.1)

        assertEquals(4_068_209.22, result.maturity, 0.005)
        assertEquals(2_250_000.0, result.deposited, 1e-9)
        assertEquals(1_818_209.22, result.interest, 0.005)
    }

    @Test
    fun `ppf lists every year and the last closing balance is the maturity`() {
        val result = DepositCalculator.ppf(150_000.0, 7.1)

        assertEquals(DepositCalculator.PPF_YEARS, result.years.size)
        val first = result.years.first()
        assertEquals(10_650.0, first.interest, 1e-6)
        assertEquals(160_650.0, first.closing, 1e-6)
        assertEquals((160_650.0 + 150_000.0) * 1.071, result.years[1].closing, 1e-6)
        assertEquals(result.maturity, result.years.last().closing, 1e-9)
    }

    @Test
    fun `ppf at zero rate gives back the deposits`() {
        assertEquals(7_500.0, DepositCalculator.ppf(500.0, 0.0).maturity, 1e-9)
    }

    @Test
    fun `ppf deposit must be within the scheme limits`() {
        assertThrows<IllegalArgumentException> { DepositCalculator.ppf(499.0, 7.1) }
        assertThrows<IllegalArgumentException> { DepositCalculator.ppf(150_001.0, 7.1) }
    }
}
