package com.calculator.feature.finance.tipsplit

import com.calculator.R
import com.calculator.core.common.input.InputResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/** Tip split fields as typed, to a split bill or a per-field error. */
class TipSplitInputTest {
    private fun input(
        bill: String = "1000",
        tip: String = "100",
        people: Int = 2,
        roundUp: Boolean = false,
    ) = tipSplitInput(bill, tip, people, roundUp, Locale.US)

    private fun error(result: InputResult<*>) = result as InputResult.Invalid

    @Test
    fun `valid fields split the bill`() {
        val ok = input() as InputResult.Ok
        assertEquals(1_100.0, ok.value.total, 0.001)
        assertEquals(550.0, ok.value.perPerson, 0.001)
    }

    @Test
    fun `round up folds the extra into the tip`() {
        val ok = input(bill = "100", tip = "0", people = 3, roundUp = true) as InputResult.Ok
        assertEquals(34.0, ok.value.perPerson, 0.001)
        assertEquals(2.0, ok.value.tip, 0.001)
    }

    @Test
    fun `empty and partial fields`() {
        assertEquals(R.string.tipsplit_bill, error(input(bill = "")).field)
        assertEquals(R.string.input_error_required, error(input(bill = "")).message)
        assertEquals(R.string.tipsplit_tip_amount_input, error(input(tip = "-")).field)
        assertTrue(input(bill = "1.") is InputResult.Ok)
        assertEquals(R.string.input_error_invalid, error(input(bill = "1.2.3")).message)
    }

    @Test
    fun `negative amounts are rejected`() {
        val bill = error(input(bill = "-5"))
        assertEquals(R.string.tipsplit_bill, bill.field)
        assertEquals(R.string.input_error_negative, bill.message)
        assertEquals(R.string.tipsplit_tip_amount_input, error(input(tip = "-1")).field)
    }

    @Test
    fun `a total past the largest number says the result is too large`() {
        val huge = "1" + "0".repeat(308)
        val error = error(input(bill = huge, tip = huge))
        assertEquals(R.string.tipsplit_bill, error.field)
        assertEquals(R.string.input_error_too_large, error.message)
    }
}
