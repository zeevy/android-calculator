package com.calculator.feature.finance.gst

import com.calculator.R
import com.calculator.core.common.input.InputResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/** GST fields as typed, to a split or a per-field error. */
class GstInputTest {
    private fun input(
        net: String = "1000",
        gross: String = "1180",
        rate: String = "18",
        intraState: Boolean = true,
        grossIsSource: Boolean = false,
    ) = gstInput(net, gross, rate, intraState, grossIsSource, Locale.US)

    private fun ok(result: InputResult<GstInput>) = (result as InputResult.Ok).value

    private fun invalid(result: InputResult<GstInput>) = result as InputResult.Invalid

    @Test
    fun `net as source fills the gross field`() {
        val out = ok(input(net = "2000", gross = "anything"))
        assertEquals(2360.0, out.result.gross, 1e-9)
        assertEquals(180.0, out.result.cgst, 1e-9)
        assertEquals("2000", out.netText)
        assertEquals("2360", out.grossText)
    }

    @Test
    fun `gross as source fills the net field`() {
        val out = ok(input(net = "", gross = "1180", intraState = false, grossIsSource = true))
        assertEquals(1000.0, out.result.net, 1e-9)
        assertEquals(180.0, out.result.igst, 1e-9)
        assertEquals("1000", out.netText)
        assertEquals("1180", out.grossText)
    }

    @Test
    fun `empty and lone minus name the source field`() {
        assertEquals(InputResult.Invalid(R.string.gst_amount_net, R.string.input_error_required), input(net = ""))
        assertEquals(
            InputResult.Invalid(R.string.gst_amount_gross, R.string.input_error_required),
            input(gross = "-", grossIsSource = true),
        )
        assertEquals(R.string.gst_rate, invalid(input(rate = "")).field)
    }

    @Test
    fun `a trailing decimal point still reads`() {
        assertEquals("1.18", ok(input(net = "1.")).grossText)
    }

    @Test
    fun `negative amount or rate is rejected`() {
        assertEquals(InputResult.Invalid(R.string.gst_amount_net, R.string.input_error_negative), input(net = "-5"))
        assertEquals(InputResult.Invalid(R.string.gst_rate, R.string.input_error_negative), input(rate = "-1"))
    }

    @Test
    fun `an overflowing gross says the result is too large`() {
        val error = invalid(input(net = "1" + "0".repeat(300), rate = "1000000000"))
        assertEquals(R.string.gst_amount_net, error.field)
        assertEquals(R.string.input_error_too_large, error.message)
    }
}
