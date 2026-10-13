package com.calculator.feature.finance.profit

import com.calculator.R
import com.calculator.core.common.input.InputResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/** Profit fields as typed, to a result or a per-field error. */
class ProfitInputTest {
    private fun input(
        mode: ProfitMode = ProfitMode.Prices,
        cost: String = "800",
        second: String = "1000",
    ) = profitInput(mode, cost, second, Locale.US)

    private fun error(result: InputResult<*>) = result as InputResult.Invalid

    @Test
    fun `valid fields give the result in each mode`() {
        assertEquals(200.0, (input() as InputResult.Ok).value.profit, 1e-9)
        assertEquals(1000.0, (input(ProfitMode.Markup, second = "25") as InputResult.Ok).value.selling, 1e-9)
        assertEquals(1000.0, (input(ProfitMode.Margin, second = "20") as InputResult.Ok).value.selling, 1e-9)
    }

    @Test
    fun `each error names the field that is wrong`() {
        assertEquals(R.string.profit_cost, error(input(cost = "")).field)
        assertEquals(R.string.profit_selling, error(input(second = "")).field)
        assertEquals(R.string.profit_markup_input, error(input(ProfitMode.Markup, second = "x")).field)
        assertEquals(R.string.profit_margin_input, error(input(ProfitMode.Margin, second = "")).field)
    }

    @Test
    fun `a margin of 100 must be below 100`() {
        val error = error(input(ProfitMode.Margin, second = "100"))
        assertEquals(R.string.profit_margin_input, error.field)
        assertEquals(R.string.input_error_below, error.message)
        assertEquals(listOf("100"), error.args)
    }

    @Test
    fun `negative values are rejected`() {
        assertEquals(R.string.input_error_negative, error(input(cost = "-1")).message)
        assertEquals(R.string.input_error_negative, error(input(second = "-1")).message)
        assertEquals(R.string.input_error_negative, error(input(ProfitMode.Markup, second = "-5")).message)
        assertEquals(R.string.input_error_negative, error(input(ProfitMode.Margin, second = "-5")).message)
    }

    @Test
    fun `a selling price that overflows says the result is too large`() {
        // About 1e308; doubling it passes Double.MAX_VALUE.
        val error = error(input(ProfitMode.Markup, cost = "9".repeat(308), second = "100"))
        assertEquals(R.string.profit_markup_input, error.field)
        assertEquals(R.string.input_error_too_large, error.message)
    }
}
