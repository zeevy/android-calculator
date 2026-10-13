package com.calculator.feature.math.percent

import com.calculator.R
import com.calculator.core.common.input.InputResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/** Percentage fields as typed, to an answer or a per-field error. */
class PercentInputTest {
    private fun input(
        mode: PercentMode,
        a: String,
        b: String,
    ) = percentInput(mode, a, b, Locale.US)

    private fun value(result: InputResult<Double>) = (result as InputResult.Ok).value

    private fun error(result: InputResult<*>) = result as InputResult.Invalid

    @Test
    fun `valid fields give the answer in each mode`() {
        assertEquals(50.0, value(input(PercentMode.PercentOf, "20", "250")), 1e-9)
        assertEquals(25.0, value(input(PercentMode.WhatPercent, "30", "120")), 1e-9)
        assertEquals(-20.0, value(input(PercentMode.Change, "100", "80")), 1e-9)
    }

    @Test
    fun `empty and partial fields`() {
        val empty = error(input(PercentMode.PercentOf, "", "250"))
        assertEquals(R.string.percent_label_percent, empty.field)
        assertEquals(R.string.input_error_required, empty.message)
        assertEquals(R.string.percent_label_to, error(input(PercentMode.Change, "80", "-")).field)
        assertTrue(input(PercentMode.WhatPercent, "1.", "4") is InputResult.Ok)
    }

    @Test
    fun `a zero base is named on the field we divide by`() {
        assertEquals(R.string.percent_label_whole, error(input(PercentMode.WhatPercent, "30", "0")).field)
        assertEquals(R.string.input_error_zero, error(input(PercentMode.WhatPercent, "30", "0")).message)
        assertEquals(R.string.percent_label_from, error(input(PercentMode.Change, "0", "100")).field)
        assertTrue(input(PercentMode.PercentOf, "20", "0") is InputResult.Ok)
    }

    @Test
    fun `an answer past the largest number says the result is too large`() {
        val huge = "1" + "0".repeat(200)
        val error = error(input(PercentMode.PercentOf, huge, huge))
        assertEquals(R.string.percent_label_value, error.field)
        assertEquals(R.string.input_error_too_large, error.message)
    }
}
