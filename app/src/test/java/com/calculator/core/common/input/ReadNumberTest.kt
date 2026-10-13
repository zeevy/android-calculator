package com.calculator.core.common.input

import com.calculator.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/** One number field read into a value or a field error. */
class ReadNumberTest {
    private val field = R.string.loan_loan_amount

    private fun read(
        text: String,
        min: Double? = null,
        max: Double? = null,
        minExclusive: Boolean = false,
        whole: Boolean = false,
    ) = readNumber(text, field, Locale.US, min, max, minExclusive, whole)

    @Test
    fun `a valid number is read`() {
        assertEquals(InputResult.Ok(1234.5), read("1,234.5"))
    }

    @Test
    fun `empty and lone minus ask for a value`() {
        assertEquals(InputResult.Invalid(field, R.string.input_error_required), read(""))
        assertEquals(InputResult.Invalid(field, R.string.input_error_required), read("-"))
    }

    @Test
    fun `text that is not a number is invalid`() {
        assertEquals(InputResult.Invalid(field, R.string.input_error_invalid), read("12x"))
    }

    @Test
    fun `whole-number fields reject fractions`() {
        assertEquals(InputResult.Invalid(field, R.string.input_error_whole), read("12.5", whole = true))
        assertEquals(InputResult.Ok(12.0), read("12", whole = true))
    }

    @Test
    fun `bounds give the matching message`() {
        assertEquals(InputResult.Invalid(field, R.string.input_error_positive), read("0", min = 0.0, minExclusive = true))
        assertEquals(InputResult.Invalid(field, R.string.input_error_negative), read("-1", min = 0.0))
        assertEquals(InputResult.Invalid(field, R.string.input_error_range, listOf("1", "600")), read("601", min = 1.0, max = 600.0))
        assertEquals(InputResult.Invalid(field, R.string.input_error_max, listOf("100")), read("101", max = 100.0))
        assertEquals(InputResult.Invalid(field, R.string.input_error_min, listOf("5")), read("4", min = 5.0))
    }
}
