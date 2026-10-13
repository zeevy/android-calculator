package com.calculator.feature.datetime.datediff

import com.calculator.R
import com.calculator.core.common.input.InputResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Locale

/** Typed day offset, to a resulting date or an error on the offset field. */
class DateDiffInputTest {
    private val start = LocalDate.of(2026, 1, 1)

    private fun input(days: String) = dateOffsetInput(start, days, Locale.US)

    private fun date(days: String) = (input(days) as InputResult.Ok).value

    private fun error(days: String) = input(days) as InputResult.Invalid

    @Test
    fun `valid offsets move the date either way`() {
        assertEquals(LocalDate.of(2026, 4, 1), date("90"))
        assertEquals(LocalDate.of(2025, 12, 31), date("-1"))
    }

    @Test
    fun `empty and partial offsets`() {
        assertEquals(R.string.datediff_offset, error("").field)
        assertEquals(R.string.input_error_required, error("").message)
        assertEquals(R.string.input_error_required, error("-").message)
        assertEquals(LocalDate.of(2026, 1, 2), date("1."))
        assertEquals(R.string.input_error_whole, error("1.5").message)
    }

    @Test
    fun `offsets past the limit are rejected before the date is moved`() {
        assertEquals(R.string.input_error_range, error("100001").message)
        assertEquals(R.string.input_error_range, error("-10000000000").message)
        assertEquals(start.plusDays(MAX_OFFSET_DAYS.toLong()), date("100000"))
    }
}
