package com.calculator.core.common.format

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class NumberFormatterTest {
    @Test
    fun `en-US uses comma grouping and period decimal`() {
        val s = NumberFormatter.format(1_234_567.89, Locale.US, maxFractionDigits = 2)
        assertEquals("1,234,567.89", s)
    }

    @Test
    fun `en-IN uses lakh-crore grouping`() {
        val s =
            NumberFormatter.format(
                1_234_567.89,
                Locale("en", "IN"),
                maxFractionDigits = 2,
            )
        // 12,34,567.89 (lakh) - 2 digits, 2 digits, 3 digits, then decimal.
        assertEquals("12,34,567.89", s)
    }

    @Test
    fun `de-DE swaps decimal and grouping separators`() {
        val s = NumberFormatter.format(1_234_567.89, Locale.GERMANY, maxFractionDigits = 2)
        assertEquals("1.234.567,89", s)
    }

    @Test
    fun `money keeps exactly two fraction digits`() {
        val s = NumberFormatter.money(1_234.5, Locale.US)
        assertEquals("1,234.50", s)
    }

    @Test
    fun `parse round-trip via the same locale`() {
        val formatted = NumberFormatter.format(987_654.321, Locale.GERMANY, maxFractionDigits = 3)
        val parsed = NumberFormatter.parseOrNull(formatted, Locale.GERMANY)
        assertEquals(987_654.321, parsed!!, 1e-9)
    }

    @Test
    fun `parseOrNull returns null on garbage`() {
        assertEquals(null, NumberFormatter.parseOrNull("not-a-number", Locale.US))
    }

    @Test
    fun `negative sign respects locale`() {
        val us = NumberFormatter.format(-1_234.5, Locale.US, maxFractionDigits = 1)
        assertEquals("-1,234.5", us)
    }
}
