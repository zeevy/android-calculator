package com.calculator.core.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class EmiCalculatorTest {
    @Test
    fun `EMI for 100k at 10pct over 12 months matches reference`() {
        val r = EmiCalculator.emi(principal = 100_000.0, annualRatePercent = 10.0, months = 12)
        // Reference: 100000 * 0.10/12 * (1+0.10/12)^12 / ((1+0.10/12)^12 - 1)
        // = 8791.589 (per any online EMI calculator).
        assertTrue(abs(r.emi - 8_791.589) < 0.01) { "got ${r.emi}" }
        assertTrue(abs(r.totalPayment - 105_499.069) < 0.05) { "got ${r.totalPayment}" }
    }

    @Test
    fun `zero-interest loan splits the principal evenly`() {
        val r = EmiCalculator.emi(principal = 12_000.0, annualRatePercent = 0.0, months = 12)
        assertEquals(1_000.0, r.emi, 1e-9)
        assertEquals(0.0, r.totalInterest, 1e-9)
        assertEquals(12_000.0, r.totalPayment, 1e-9)
    }

    @Test
    fun `amortisation balance lands at zero on the last row`() {
        val r = EmiCalculator.emi(principal = 50_000.0, annualRatePercent = 12.0, months = 24)
        assertEquals(0.0, r.amortisation.last().balanceAfter, 1e-6)
    }

    @Test
    fun `amortisation rows sum to the total payment within rounding`() {
        val r = EmiCalculator.emi(principal = 200_000.0, annualRatePercent = 7.5, months = 60)
        val sumPaid =
            r.amortisation.sumOf { it.principalPaid + it.interestPaid }
        assertTrue(abs(sumPaid - r.totalPayment) < 1.0) {
            "sum=$sumPaid total=${r.totalPayment}"
        }
    }

    @Test
    fun `negative principal is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            EmiCalculator.emi(-1.0, 10.0, 12)
        }
    }

    @Test
    fun `zero months is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            EmiCalculator.emi(1_000.0, 10.0, 0)
        }
    }
}
