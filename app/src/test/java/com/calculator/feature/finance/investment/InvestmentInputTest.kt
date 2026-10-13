package com.calculator.feature.finance.investment

import com.calculator.R
import com.calculator.core.common.input.InputResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/** Investment fields as typed, to a maturity value or a per-field error. */
class InvestmentInputTest {
    private fun input(
        lumpSum: Boolean = false,
        amount: String = "5000",
        rate: String = "12",
        years: String = "10",
    ) = investmentInput(lumpSum, amount, rate, years, Locale.US)

    private fun error(result: InputResult<*>) = result as InputResult.Invalid

    @Test
    fun `valid SIP fields give the maturity`() {
        val ok = input() as InputResult.Ok
        assertEquals(600_000.0, ok.value.invested, 0.001)
        assertEquals(1_161_695.38, ok.value.maturity, 0.01)
    }

    @Test
    fun `valid lump sum fields give the maturity`() {
        val ok = input(lumpSum = true, amount = "100000", rate = "10", years = "2") as InputResult.Ok
        assertEquals(121_000.0, ok.value.maturity, 0.01)
    }

    @Test
    fun `the amount error is named by the label of the current mode`() {
        assertEquals(R.string.investment_monthly, error(input(amount = "")).field)
        assertEquals(R.string.investment_principal, error(input(lumpSum = true, amount = "")).field)
    }

    @Test
    fun `empty and partial fields`() {
        val rate = error(input(rate = "-"))
        assertEquals(R.string.investment_rate, rate.field)
        assertEquals(R.string.input_error_required, rate.message)
        assertTrue(input(years = "1.") is InputResult.Ok)
        assertEquals(R.string.investment_years, error(input(years = "")).field)
    }

    @Test
    fun `out of range fields are rejected`() {
        assertEquals(R.string.input_error_negative, error(input(amount = "-1")).message)
        assertEquals(R.string.input_error_negative, error(input(rate = "-2")).message)
        val years = error(input(years = "0"))
        assertEquals(R.string.investment_years, years.field)
        assertEquals(R.string.input_error_positive, years.message)
    }

    @Test
    fun `a tenure that overflows the maturity says the result is too large`() {
        val error = error(input(years = "100000"))
        assertEquals(R.string.investment_years, error.field)
        assertEquals(R.string.input_error_too_large, error.message)
    }

    @Test
    fun `valid CAGR fields give the growth rate`() {
        val ok = cagrInput("100000", "200000", "5", Locale.US) as InputResult.Ok
        assertEquals(14.87, ok.value.cagrPercent, 0.005)
    }

    @Test
    fun `CAGR rejects a start value of 0 and a tenure of 0`() {
        val start = error(cagrInput("0", "200000", "5", Locale.US))
        assertEquals(R.string.investment_start, start.field)
        assertEquals(R.string.input_error_positive, start.message)
        assertEquals(R.string.investment_years, error(cagrInput("100", "200", "0", Locale.US)).field)
        assertEquals(R.string.investment_end, error(cagrInput("100", "-1", "5", Locale.US)).field)
    }

    @Test
    fun `CAGR that overflows says the result is too large`() {
        val error = error(cagrInput("1", "1000000", "0.0001", Locale.US))
        assertEquals(R.string.investment_years, error.field)
        assertEquals(R.string.input_error_too_large, error.message)
    }
}
