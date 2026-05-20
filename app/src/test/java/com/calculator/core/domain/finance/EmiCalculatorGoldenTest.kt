package com.calculator.core.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.abs

/**
 * Golden-value catalogue for [EmiCalculator].
 *
 * Each row is `(principal, annualRatePercent, months, expectedEmi)`,
 * cross-checked against a standard EMI formula or a publicly available
 * online calculator. Tolerance is `0.5` rupees - well inside what any
 * UI would round to.
 */
class EmiCalculatorGoldenTest {
    @DisplayName("EMI matches reference within 0.5 of a currency unit")
    @ParameterizedTest(name = "P={0} rate={1}% n={2} -> EMI≈{3}")
    @CsvSource(
        // Classic personal-loan-shape examples
        "100000,   10.0,   12,   8791.59",
        "100000,   10.0,   24,   4614.49",
        "100000,   10.0,   60,   2124.70",
        "500000,   8.5,    60,   10258.27",
        "1000000,  9.0,    120,  12667.58",
        "200000,   7.5,    36,   6221.24",
        "50000,    12.0,   24,   2353.69",
        // High-rate, short-term
        "10000,    24.0,   6,    1785.26",
        "25000,    18.0,   12,   2291.92",
        // Long-tenor home-loan shape
        "5000000,  8.0,    240,  41822.10",
        "5000000,  8.0,    360,  36688.27",
        "7500000,  9.5,    300,  65527.25",
        // Tiny principal
        "1000,     5.0,    12,   85.61",
        "5000,     0.5,    24,   209.46",
        // Boundary rates
        "100000,   0.01,   12,   8333.79",
        "100000,   1.0,    12,   8378.86",
        "100000,   100.0,  12,   13499.58",
    )
    fun emiMatchesReference(
        principal: Double,
        rate: Double,
        months: Int,
        expectedEmi: Double,
    ) {
        val r = EmiCalculator.emi(principal = principal, annualRatePercent = rate, months = months)
        assert(abs(r.emi - expectedEmi) < 0.5) {
            "P=$principal rate=$rate n=$months: expected EMI≈$expectedEmi but got ${r.emi}"
        }
    }

    @DisplayName("zero-rate loan splits principal evenly across all tenors")
    @ParameterizedTest(name = "P={0} months={1}")
    @CsvSource(
        "12000,    12",
        "60000,    24",
        "100000,   60",
        "1,        7",
        "999999,   333",
    )
    fun zeroInterestSplitsEvenly(principal: Double, months: Int) {
        val r = EmiCalculator.emi(principal, 0.0, months)
        assertEquals(principal / months, r.emi, 1e-6)
        assertEquals(0.0, r.totalInterest, 1e-6)
        assertEquals(principal, r.totalPayment, 1e-6)
    }

    @DisplayName("amortisation invariants hold")
    @ParameterizedTest(name = "P={0} rate={1}% n={2}")
    @CsvSource(
        "100000,   10.0,   12",
        "500000,   8.5,    60",
        "1000000,  9.0,    120",
        "5000000,  8.0,    240",
        "100000,   0.0,    12",
        "100000,   24.0,   6",
    )
    fun amortisationInvariants(principal: Double, rate: Double, months: Int) {
        val r = EmiCalculator.emi(principal, rate, months)
        // Last row's balanceAfter is essentially zero
        assert(abs(r.amortisation.last().balanceAfter) < 1.0) {
            "expected final balance ≈ 0, got ${r.amortisation.last().balanceAfter}"
        }
        // Sum of principalPaid + interestPaid equals totalPayment (within rounding)
        val sumPaid = r.amortisation.sumOf { it.principalPaid + it.interestPaid }
        assert(abs(sumPaid - r.totalPayment) < 1.0) {
            "amortisation sum=$sumPaid does not equal totalPayment=${r.totalPayment}"
        }
        // Sum of principal-paid equals principal
        val sumPrincipal = r.amortisation.sumOf { it.principalPaid }
        assert(abs(sumPrincipal - principal) < 1.0) {
            "principal sum=$sumPrincipal != principal=$principal"
        }
        // Row count equals months
        assertEquals(months, r.amortisation.size)
    }

    @DisplayName("rejects invalid inputs")
    @Test
    fun rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException::class.java) { EmiCalculator.emi(-1.0, 10.0, 12) }
        assertThrows(IllegalArgumentException::class.java) { EmiCalculator.emi(0.0, 10.0, 12) }
        assertThrows(IllegalArgumentException::class.java) { EmiCalculator.emi(1000.0, 10.0, 0) }
        assertThrows(IllegalArgumentException::class.java) { EmiCalculator.emi(1000.0, 10.0, -5) }
        assertThrows(IllegalArgumentException::class.java) { EmiCalculator.emi(1000.0, -1.0, 12) }
    }
}
