package com.calculator.core.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GstCalculatorTest {
    @Test
    fun `intra-state 1000 at 18 percent splits CGST SGST`() {
        val r = GstCalculator.forward(net = 1_000.0, ratePercent = 18.0, intraState = true)
        assertEquals(90.0, r.cgst, 1e-9)
        assertEquals(90.0, r.sgst, 1e-9)
        assertEquals(0.0, r.igst, 1e-9)
        assertEquals(1_180.0, r.gross, 1e-9)
    }

    @Test
    fun `inter-state 1000 at 18 percent rolls into IGST`() {
        val r = GstCalculator.forward(net = 1_000.0, ratePercent = 18.0, intraState = false)
        assertEquals(180.0, r.igst, 1e-9)
        assertEquals(0.0, r.cgst, 1e-9)
        assertEquals(0.0, r.sgst, 1e-9)
        assertEquals(1_180.0, r.gross, 1e-9)
    }

    @Test
    fun `reverse 1180 at 18 percent recovers the 1000 net`() {
        val r = GstCalculator.reverse(gross = 1_180.0, ratePercent = 18.0, intraState = true)
        assertEquals(1_000.0, r.net, 1e-9)
        assertEquals(90.0, r.cgst, 1e-9)
    }

    @Test
    fun `zero rate is a passthrough`() {
        val r = GstCalculator.forward(net = 500.0, ratePercent = 0.0, intraState = true)
        assertEquals(500.0, r.gross, 1e-9)
        assertEquals(0.0, r.cgst, 1e-9)
    }
}
