package com.calculator.core.common.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * Locale-aware number formatting at the UI boundary.
 *
 * The math engine works in canonical (period-separated, no-grouping)
 * representations - that's important so the same expression evaluates
 * identically in every locale. This utility flips numbers *into* the
 * user's locale for display only, and parses user input back into the
 * canonical form before it touches the engine.
 *
 * Locale-specific differences this covers:
 *  - Decimal separator: `.` in en-US/INR/JP, `,` in de/fr.
 *  - Grouping separator: `,` in en-US, `,` (but at 2/3/3 digits) in
 *    en-IN (lakh / crore grouping), `.` in de.
 *  - Negative sign placement.
 *
 * Examples:
 *  - `format(1234567.89, Locale.US)`   → `"1,234,567.89"`
 *  - `format(1234567.89, Locale("en","IN"))` → `"12,34,567.89"`
 *  - `format(1234567.89, Locale.GERMANY)`    → `"1.234.567,89"`
 */
object NumberFormatter {
    /**
     * Render [value] with up to [maxFractionDigits] decimals, trimming
     * meaningless trailing zeros. Grouping is enabled.
     */
    fun format(
        value: Double,
        locale: Locale = Locale.getDefault(),
        minFractionDigits: Int = 0,
        maxFractionDigits: Int = 6,
    ): String {
        if (locale.country == "IN" && locale.language in setOf("en", "hi")) {
            // Lakh / crore grouping (2-2-3 from the right). Java's
            // DecimalFormat only supports a single groupingSize, so we
            // format the fractional half via DecimalFormat and join the
            // integer half by hand. ICU on Android would also handle
            // this, but doing it manually keeps the unit test and the
            // device output identical.
            return formatIndian(value, locale, minFractionDigits, maxFractionDigits)
        }
        val nf = NumberFormat.getNumberInstance(locale)
        nf.minimumFractionDigits = minFractionDigits
        nf.maximumFractionDigits = maxFractionDigits
        nf.isGroupingUsed = true
        return nf.format(value)
    }

    private fun formatIndian(
        value: Double,
        locale: Locale,
        minFractionDigits: Int,
        maxFractionDigits: Int,
    ): String {
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val absValue = kotlin.math.abs(value)
        val intPart = absValue.toLong()
        val fracPart = absValue - intPart

        // Integer half with 2-2-3 grouping from the right.
        val intStr = intPart.toString()
        val grouped = StringBuilder()
        val sep = symbols.groupingSeparator
        if (intStr.length <= 3) {
            grouped.append(intStr)
        } else {
            grouped.append(intStr.substring(intStr.length - 3))
            var i = intStr.length - 3
            while (i > 0) {
                val start = maxOf(i - 2, 0)
                grouped.insert(0, sep)
                grouped.insert(0, intStr.substring(start, i))
                i = start
            }
        }

        // Fractional half via a small DecimalFormat so we get
        // locale-correct decimal separator + trailing-zero trim.
        val fracText =
            if (maxFractionDigits > 0) {
                val hashes = "#".repeat(maxFractionDigits)
                val df = DecimalFormat("0.$hashes", symbols)
                df.minimumFractionDigits = minFractionDigits
                df.format(fracPart).removePrefix("0")
            } else {
                ""
            }

        val sign = if (value < 0) "-" else ""
        return sign + grouped.toString() + fracText
    }

    /**
     * Render a currency-style two-decimal amount (e.g. `1,234.50`),
     * grouping separator + always two decimals, no currency symbol -
     * the caller is responsible for prefixing/suffixing the code.
     */
    fun money(value: Double, locale: Locale = Locale.getDefault()): String {
        val format =
            DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(locale))
        return format.format(value)
    }

    /**
     * Parse a locale-formatted number back to a Double. Returns null
     * for anything the locale's [NumberFormat] doesn't accept - keeps
     * the call site simple (`parse(text) ?: return`).
     */
    fun parseOrNull(text: String, locale: Locale = Locale.getDefault()): Double? =
        runCatching {
            NumberFormat.getNumberInstance(locale).parse(text)?.toDouble()
        }.getOrNull()
}
