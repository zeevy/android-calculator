package com.calculator.core.common.format

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
                Locale.forLanguageTag("en-IN"),
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

    @ParameterizedTest
    @ValueSource(strings = ["en-US", "en-IN", "de-DE", "fr-FR"])
    fun `parse of format gives the same value back`(tag: String) {
        val locale = Locale.forLanguageTag(tag)
        for (x in listOf(0.5, 12.0, 987_654.32, 1_234_567.89, -42_000.75)) {
            val shown = NumberFormatter.format(x, locale, maxFractionDigits = 2)
            assertEquals(x, NumberFormatter.parseNumberInput(shown, locale)!!, 1e-9, "$tag $shown")
            val typed = NumberFormatter.formatForInput(x, locale)
            assertEquals(x, NumberFormatter.parseNumberInput(typed, locale)!!, 1e-9, "$tag $typed")
        }
    }

    @Test
    fun `parseNumberInput uses the locale decimal separator`() {
        assertEquals(10.5, NumberFormatter.parseNumberInput("10,5", Locale.GERMANY))
        assertEquals(1000.0, NumberFormatter.parseNumberInput("1.000", Locale.GERMANY))
        assertEquals(10.5, NumberFormatter.parseNumberInput("10.5", Locale.US))
        assertEquals(1234567.0, NumberFormatter.parseNumberInput("12,34,567", Locale.forLanguageTag("en-IN")))
    }

    @Test
    fun `parseNumberInput rejects misplaced grouping and trailing junk`() {
        assertNull(NumberFormatter.parseNumberInput("10.5", Locale.GERMANY))
        assertNull(NumberFormatter.parseNumberInput("1,23", Locale.US))
        assertNull(NumberFormatter.parseNumberInput("12abc", Locale.US))
        assertNull(NumberFormatter.parseNumberInput("not-a-number", Locale.US))
    }

    @Test
    fun `parseNumberInput handles mid-edit text`() {
        assertNull(NumberFormatter.parseNumberInput("", Locale.US))
        assertNull(NumberFormatter.parseNumberInput("-", Locale.US))
        assertEquals(1.0, NumberFormatter.parseNumberInput("1.", Locale.US))
        assertEquals(1.0, NumberFormatter.parseNumberInput("1,", Locale.GERMANY))
        assertEquals(-3.0, NumberFormatter.parseNumberInput("-3", Locale.US))
    }

    @Test
    fun `money uses lakh grouping in en-IN`() {
        assertEquals("12,34,567.89", NumberFormatter.money(1_234_567.891, Locale.forLanguageTag("en-IN")))
        assertEquals("0.50", NumberFormatter.money(0.5, Locale.forLanguageTag("en-IN")))
    }

    @Test
    fun `en-IN rounding carries into the integer part`() {
        assertEquals("1,000", NumberFormatter.format(999.999, Locale.forLanguageTag("en-IN"), maxFractionDigits = 2))
    }

    @Test
    fun `en-IN keeps every digit beyond the Long range`() {
        assertEquals("10,00,00,00,00,00,00,00,00,000", NumberFormatter.format(1e20, Locale.forLanguageTag("en-IN")))
    }

    @Test
    fun `non-finite values do not crash the formatter`() {
        assertEquals(
            NumberFormatter.format(Double.POSITIVE_INFINITY, Locale.US),
            NumberFormatter.format(Double.POSITIVE_INFINITY, Locale.forLanguageTag("en-IN")),
        )
    }

    @Test
    fun `extractPastedNumber reads the number in the device locale`() {
        assertEquals("1234.5", NumberFormatter.extractPastedNumber("Total ₹1,234.50 only", Locale.US))
        assertEquals("1234.5", NumberFormatter.extractPastedNumber("1.234,50 €", Locale.GERMANY))
        assertEquals("-7", NumberFormatter.extractPastedNumber("-7 apples", Locale.US))
        assertNull(NumberFormatter.extractPastedNumber("no digits", Locale.US))
    }

    @Test
    fun `negative sign respects locale`() {
        val us = NumberFormatter.format(-1_234.5, Locale.US, maxFractionDigits = 1)
        assertEquals("-1,234.5", us)
    }

    @Test
    fun `paste reads the app's own copied form in a comma-decimal locale`() {
        assertEquals("0.25", NumberFormatter.extractPastedNumber("0.25", Locale.GERMANY))
        assertEquals("1.5", NumberFormatter.extractPastedNumber("1.5+2 = 3.5", Locale.GERMANY))
        assertEquals("1234.5", NumberFormatter.extractPastedNumber("1.234,5", Locale.GERMANY))
    }

    @Test
    fun `en-IN rounds money like the other locales on a device`() {
        assertEquals("2.68", NumberFormatter.money(2.675, Locale.forLanguageTag("en-IN")))
    }

    @Test
    fun `every Indian locale accepts lakh grouping`() {
        assertEquals(100000.0, NumberFormatter.parseNumberInput("1,00,000", Locale.forLanguageTag("ta-IN")))
        assertEquals("1,00,000", NumberFormatter.format(100_000.0, Locale.forLanguageTag("ta-IN")))
    }

    @Test
    fun `stray characters are rejected`() {
        assertNull(NumberFormatter.parseNumberInput("1 0", Locale.US))
        assertNull(NumberFormatter.parseNumberInput("1'000", Locale.US))
    }
}
