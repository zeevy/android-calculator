package com.calculator.core.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Golden catalogue for [GstCalculator]. Covers:
 *  - forward and reverse directions at every Indian GST slab
 *    (5/12/18/28) plus a few custom rates
 *  - intra-state (CGST+SGST split equally) vs inter-state (IGST only)
 *  - zero amount / zero rate
 *  - invalid input rejection
 */
class GstCalculatorGoldenTest {
    /** Net + rate → gross + tax split (intra-state). */
    @DisplayName("forward intra-state: CGST = SGST = tax/2")
    @ParameterizedTest(name = "net={0} rate={1}% -> gross={2}, half={3}")
    @CsvSource(
        // Standard Indian GST slabs
        "100,      5.0,      105,      2.5",
        "100,      12.0,     112,      6.0",
        "100,      18.0,     118,      9.0",
        "100,      28.0,     128,      14.0",
        // Higher values
        "1000,     18.0,     1180,     90.0",
        "10000,    18.0,     11800,    900.0",
        "999,      18.0,     1178.82,  89.91",
        // Custom rates
        "1000,     0.5,      1005,     2.5",
        "1000,     1.0,      1010,     5.0",
        "1000,     50.0,     1500,     250.0",
        // Boundary - zero values
        "0,        18.0,     0,        0",
        "100,      0.0,      100,      0",
    )
    fun forwardIntraState(net: Double, rate: Double, expectedGross: Double, expectedHalf: Double) {
        val r = GstCalculator.forward(net = net, ratePercent = rate, intraState = true)
        assertEquals(expectedGross, r.gross, 0.01)
        assertEquals(expectedHalf, r.cgst, 0.01)
        assertEquals(expectedHalf, r.sgst, 0.01)
        assertEquals(0.0, r.igst, 0.01)
        // Self-consistency: net + cgst + sgst + igst == gross
        assertEquals(r.gross, r.net + r.cgst + r.sgst + r.igst, 0.01)
    }

    /** Net + rate → gross + tax (inter-state, IGST only). */
    @DisplayName("forward inter-state: full tax goes to IGST")
    @ParameterizedTest(name = "net={0} rate={1}% -> gross={2}, IGST={3}")
    @CsvSource(
        "100,      5.0,      105,      5.0",
        "100,      12.0,     112,      12.0",
        "100,      18.0,     118,      18.0",
        "100,      28.0,     128,      28.0",
        "1000,     18.0,     1180,     180.0",
        "0,        18.0,     0,        0",
        "100,      0.0,      100,      0",
    )
    fun forwardInterState(net: Double, rate: Double, expectedGross: Double, expectedIgst: Double) {
        val r = GstCalculator.forward(net = net, ratePercent = rate, intraState = false)
        assertEquals(expectedGross, r.gross, 0.01)
        assertEquals(0.0, r.cgst, 0.01)
        assertEquals(0.0, r.sgst, 0.01)
        assertEquals(expectedIgst, r.igst, 0.01)
    }

    /** Reverse: gross + rate → net + tax. */
    @DisplayName("reverse: gross / (1 + rate/100) = net")
    @ParameterizedTest(name = "gross={0} rate={1}% -> net={2}")
    @CsvSource(
        "105,      5.0,      100",
        "112,      12.0,     100",
        "118,      18.0,     100",
        "128,      28.0,     100",
        "1180,     18.0,     1000",
        "11800,    18.0,     10000",
        "0,        18.0,     0",
        // Reverse with 0% rate: net == gross
        "500,      0.0,      500",
    )
    fun reverseInferNet(gross: Double, rate: Double, expectedNet: Double) {
        val r = GstCalculator.reverse(gross = gross, ratePercent = rate, intraState = true)
        assertEquals(expectedNet, r.net, 0.01)
        assertEquals(gross, r.gross, 0.01)
    }

    /** Round-trip: forward then reverse must land back on the same net. */
    @DisplayName("forward then reverse round-trips to original net")
    @ParameterizedTest(name = "net={0} rate={1}%")
    @CsvSource(
        "100,      5.0",
        "100,      18.0",
        "1000,     12.0",
        "9999.99,  28.0",
        "0,        18.0",
    )
    fun roundTripNetGrossNet(net: Double, rate: Double) {
        val forward = GstCalculator.forward(net = net, ratePercent = rate, intraState = true)
        val reverse = GstCalculator.reverse(forward.gross, rate, intraState = true)
        assertEquals(net, reverse.net, 0.01)
    }

    @DisplayName("rejects invalid inputs")
    @org.junit.jupiter.api.Test
    fun rejectsInvalid() {
        assertThrows(IllegalArgumentException::class.java) {
            GstCalculator.forward(net = -1.0, ratePercent = 18.0, intraState = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GstCalculator.forward(net = 100.0, ratePercent = -1.0, intraState = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GstCalculator.reverse(gross = -1.0, ratePercent = 18.0, intraState = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GstCalculator.reverse(gross = 100.0, ratePercent = -1.0, intraState = true)
        }
    }
}
