package com.calculator.feature.finance.discount

import com.calculator.R
import com.calculator.core.common.input.InputResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/** Discount fields as typed, to a result or a per-field error. */
class DiscountInputTest {
    private fun input(
        mrp: String = "2000",
        percent: String = "20",
        final: String = "1600",
        finalIsSource: Boolean = false,
    ) = discountInput(mrp, percent, final, finalIsSource, Locale.US)

    private fun ok(result: InputResult<DiscountInput>) = (result as InputResult.Ok).value

    private fun error(result: InputResult<DiscountInput>) = (result as InputResult.Invalid).run { field to message }

    @Test
    fun `percent as source fills the final price`() {
        val out = ok(input(percent = "25", final = "anything"))
        assertEquals(500.0, out.result.savings, 1e-9)
        assertEquals("25", out.percentText)
        assertEquals("1500", out.finalText)
    }

    @Test
    fun `final price as source fills the percent`() {
        val out = ok(input(percent = "", final = "1500", finalIsSource = true))
        assertEquals(500.0, out.result.savings, 1e-9)
        assertEquals("25", out.percentText)
        assertEquals("1500", out.finalText)
    }

    @Test
    fun `empty and lone minus name the field`() {
        assertEquals(R.string.discount_mrp to R.string.input_error_required, error(input(mrp = "")))
        assertEquals(R.string.discount_percent to R.string.input_error_required, error(input(percent = "-")))
        assertEquals(
            R.string.discount_final to R.string.input_error_required,
            error(input(final = "", finalIsSource = true)),
        )
    }

    @Test
    fun `a trailing decimal point still reads`() {
        assertEquals("1600", ok(input(percent = "20.")).finalText)
        assertEquals("20", ok(input(final = "1600.", finalIsSource = true)).percentText)
    }

    @Test
    fun `values out of range name the field`() {
        assertEquals(R.string.discount_percent to R.string.input_error_range, error(input(percent = "120")))
        assertEquals(
            R.string.discount_final to R.string.input_error_range,
            error(input(final = "2500", finalIsSource = true)),
        )
        assertEquals(R.string.discount_mrp to R.string.input_error_negative, error(input(mrp = "-1")))
    }

    @Test
    fun `zero MRP works for a percent but not for a final price`() {
        assertEquals("0", ok(input(mrp = "0")).finalText)
        assertEquals(
            R.string.discount_mrp to R.string.input_error_positive,
            error(input(mrp = "0", final = "0", finalIsSource = true)),
        )
    }
}
