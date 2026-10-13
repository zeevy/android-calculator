package com.calculator.feature.finance.deposit

import com.calculator.R
import com.calculator.core.common.input.InputResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/** Deposit fields as typed, to a maturity or a per-field error. */
class DepositInputTest {
    private fun error(result: InputResult<*>) = result as InputResult.Invalid

    @Test
    fun `default fields give a result in every mode`() {
        assertEquals(141_477.82, (fdInput(FdFields(), Locale.US) as InputResult.Ok).value.maturity, 0.005)
        assertEquals(12_462.13, (rdInput(RdFields(), Locale.US) as InputResult.Ok).value.maturity, 0.005)
        assertEquals(4_068_209.22, (ppfInput(PpfFields(), Locale.US) as InputResult.Ok).value.maturity, 0.005)
    }

    @Test
    fun `fd years and months add up to the tenure`() {
        val ok = fdInput(FdFields(years = "1", months = "2"), Locale.US) as InputResult.Ok
        assertEquals(108_436.41, ok.value.maturity, 0.005)
    }

    @Test
    fun `fd with no tenure points at the months field`() {
        val error = error(fdInput(FdFields(years = "0", months = "0"), Locale.US))
        assertEquals(R.string.deposit_months, error.field)
        assertEquals(R.string.input_error_positive, error.message)
    }

    @Test
    fun `fd months above 11 and part years are rejected`() {
        val months = error(fdInput(FdFields(months = "12"), Locale.US))
        assertEquals(R.string.deposit_months, months.field)
        assertEquals(R.string.input_error_range, months.message)
        assertEquals(R.string.input_error_whole, error(fdInput(FdFields(years = "1.5"), Locale.US)).message)
    }

    @Test
    fun `rd tenure must be a whole number of months from 1 to 600`() {
        assertEquals(R.string.deposit_tenure, error(rdInput(RdFields(months = "0"), Locale.US)).field)
        assertEquals(R.string.input_error_range, error(rdInput(RdFields(months = "601"), Locale.US)).message)
        assertEquals(R.string.deposit_monthly, error(rdInput(RdFields(amount = "-5"), Locale.US)).field)
    }

    @Test
    fun `ppf deposit outside 500 to 150000 shows the range`() {
        val low = error(ppfInput(PpfFields(amount = "400"), Locale.US))
        assertEquals(R.string.deposit_yearly, low.field)
        assertEquals(R.string.input_error_range, low.message)
        assertEquals(listOf("500", "150,000"), low.args)
        assertEquals(R.string.deposit_yearly, error(ppfInput(PpfFields(amount = "150001"), Locale.US)).field)
    }

    @Test
    fun `a rate that overflows says the result is too large`() {
        val error = error(fdInput(FdFields(rate = "10000000", years = "100"), Locale.US))
        assertEquals(R.string.deposit_rate, error.field)
        assertEquals(R.string.input_error_too_large, error.message)
    }

    @Test
    fun `the PPF starting rate reads back on a German phone`() {
        val saved = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
        try {
            val ok = ppfInput(PpfFields(), Locale.GERMANY) as InputResult.Ok
            assertEquals(4_068_209.22, ok.value.maturity, 0.005)
        } finally {
            Locale.setDefault(saved)
        }
    }
}
