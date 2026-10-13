package com.calculator.feature.finance.loan

import com.calculator.R
import com.calculator.core.common.input.InputResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/** Loan fields as typed, to an EMI or a per-field error. */
class LoanInputTest {
    private fun input(
        principal: String = "100000",
        rate: String = "10",
        months: String = "12",
    ) = loanInput(principal, rate, months, Locale.US)

    @Test
    fun `valid fields give the EMI`() {
        val ok = input() as InputResult.Ok
        assertEquals(8_791.59, ok.value.emi, 0.01)
    }

    @Test
    fun `each wrong field is named`() {
        assertEquals(R.string.loan_loan_amount, (input(principal = "") as InputResult.Invalid).field)
        assertEquals(R.string.loan_loan_amount, (input(principal = "0") as InputResult.Invalid).field)
        assertEquals(R.string.loan_annual_rate, (input(rate = "-2") as InputResult.Invalid).field)
        assertTrue(input(months = "12.") is InputResult.Ok)
        assertEquals(R.string.loan_tenure, (input(months = "12.5") as InputResult.Invalid).field)
    }

    @Test
    fun `tenure above the cap is rejected before any schedule is built`() {
        val error = input(months = "1000000") as InputResult.Invalid
        assertEquals(R.string.loan_tenure, error.field)
        assertEquals(R.string.input_error_range, error.message)
    }

    @Test
    fun `a rate that overflows the EMI says the result is too large`() {
        val error = input(rate = "1000000000", months = "600") as InputResult.Invalid
        assertEquals(R.string.input_error_too_large, error.message)
        assertTrue(error.field == R.string.loan_annual_rate)
    }
}
