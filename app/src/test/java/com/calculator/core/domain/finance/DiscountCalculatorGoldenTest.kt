package com.calculator.core.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Golden catalogue for [DiscountCalculator]. Forward + reverse +
 * round-trip + boundary cases.
 */
class DiscountCalculatorGoldenTest {
    @DisplayName("forward: mrp - mrp×pct/100 = finalPrice")
    @ParameterizedTest(name = "mrp={0} pct={1}% -> final={2}, savings={3}")
    @CsvSource(
        // Common retail discounts
        "1000,    10,    900,    100",
        "1000,    20,    800,    200",
        "1000,    25,    750,    250",
        "1000,    50,    500,    500",
        "1000,    75,    250,    750",
        // Boundary percents
        "1000,    0,     1000,   0",
        "1000,    100,   0,      1000",
        // Common shopping prices
        "499,     10,    449.1,  49.9",
        "2999,    30,    2099.3, 899.7",
        "599,     33.33, 399.35, 199.65",
        // Tiny / large MRPs
        "1,       50,    0.5,    0.5",
        "100000,  15,    85000,  15000",
        "1000000, 5,     950000, 50000",
        // Zero MRP
        "0,       50,    0,      0",
    )
    fun forwardDiscount(mrp: Double, pct: Double, expectedFinal: Double, expectedSavings: Double) {
        val r = DiscountCalculator.forward(mrp = mrp, percentOff = pct)
        assertEquals(expectedFinal, r.finalPrice, 0.01)
        assertEquals(expectedSavings, r.savings, 0.01)
        assertEquals(mrp, r.mrp, 0.01)
        assertEquals(pct, r.percentOff, 0.01)
    }

    @DisplayName("reverse: (mrp - finalPrice)/mrp × 100 = percentOff")
    @ParameterizedTest(name = "mrp={0} final={1} -> pct={2}%")
    @CsvSource(
        "1000,    900,    10",
        "1000,    800,    20",
        "1000,    500,    50",
        "1000,    250,    75",
        "1000,    0,      100",
        // Realistic store flows
        "2999,    2099.3, 30",
        "499,     449.1,  10",
        "5000,    3750,   25",
        // No discount applied
        "1000,    1000,   0",
        // Tiny MRP
        "1,       0.5,    50",
    )
    fun reverseDiscount(mrp: Double, finalPrice: Double, expectedPct: Double) {
        val r = DiscountCalculator.reverse(mrp = mrp, finalPrice = finalPrice)
        assertEquals(expectedPct, r.percentOff, 0.05)
        assertEquals(mrp - finalPrice, r.savings, 0.01)
    }

    @DisplayName("forward then reverse round-trips to the same percent")
    @ParameterizedTest(name = "mrp={0} pct={1}%")
    @CsvSource(
        "1000,    10",
        "2999,    30",
        "499,     33.33",
        "100000,  15",
        "1,       50",
    )
    fun roundTrip(mrp: Double, pct: Double) {
        val forward = DiscountCalculator.forward(mrp, pct)
        val reverse = DiscountCalculator.reverse(mrp, forward.finalPrice)
        assertEquals(pct, reverse.percentOff, 0.05)
    }

    @DisplayName("rejects invalid inputs")
    @org.junit.jupiter.api.Test
    fun rejectsInvalidInputs() {
        // Forward: negative MRP / out-of-range percent
        assertThrows(IllegalArgumentException::class.java) {
            DiscountCalculator.forward(mrp = -1.0, percentOff = 10.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DiscountCalculator.forward(mrp = 100.0, percentOff = -1.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DiscountCalculator.forward(mrp = 100.0, percentOff = 101.0)
        }
        // Reverse: final > mrp, negative final, zero mrp
        assertThrows(IllegalArgumentException::class.java) {
            DiscountCalculator.reverse(mrp = 100.0, finalPrice = 150.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DiscountCalculator.reverse(mrp = 100.0, finalPrice = -1.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DiscountCalculator.reverse(mrp = 0.0, finalPrice = 0.0)
        }
    }
}
