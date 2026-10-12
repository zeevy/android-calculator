package com.calculator.core.common.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale

/**
 * Locale-aware number formatting at the UI boundary.
 *
 * The math engine works in canonical (period-separated, no-grouping)
 * representations - that's important so the same expression evaluates
 * identically in every locale. This utility flips numbers *into* the
 * user's locale for display only, and parses user input back into a
 * number with [parseNumberInput] before it touches the engine. Every
 * number field in the app goes through these two functions.
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
        if (value.isFinite() && locale.country == "IN" && locale.language in setOf("en", "hi")) {
            // Lakh / crore grouping (2-2-3 from the right). Java's
            // DecimalFormat only supports a single groupingSize, so the
            // integer half is grouped by hand. ICU on Android would also
            // handle this, but doing it manually keeps the unit test and
            // the device output identical.
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
        // Round once on the exact value (HALF_EVEN, like NumberFormat), so
        // a fraction that rounds up carries into the integer part and
        // values beyond the Long range keep every digit.
        val rounded = BigDecimal(value).abs().setScale(maxFractionDigits, RoundingMode.HALF_EVEN)
        val plain = rounded.toPlainString()
        val intStr = plain.substringBefore('.')
        val frac = plain.substringAfter('.', "").trimEnd('0').padEnd(minFractionDigits, '0')
        val sign = if (value < 0 && rounded.signum() != 0) symbols.minusSign.toString() else ""
        val fracText = if (frac.isEmpty()) "" else symbols.decimalSeparator + frac
        return sign + groupIndian(intStr, symbols.groupingSeparator) + fracText
    }

    /** "1234567" to "12,34,567": the last three digits, then pairs. */
    private fun groupIndian(
        digits: String,
        separator: Char,
    ): String {
        if (digits.length <= INDIAN_LAST_GROUP) return digits
        val head = digits.dropLast(INDIAN_LAST_GROUP)
        val pairs = head
            .reversed()
            .chunked(2)
            .map { it.reversed() }
            .reversed()
        return (pairs + digits.takeLast(INDIAN_LAST_GROUP)).joinToString(separator.toString())
    }

    /**
     * Render a currency-style two-decimal amount (e.g. `1,234.50`, or
     * `12,34,567.89` in en-IN), grouping separator + always two decimals,
     * no currency symbol - the caller is responsible for the code.
     */
    fun money(
        value: Double,
        locale: Locale = Locale.getDefault(),
    ): String = format(value, locale, minFractionDigits = 2, maxFractionDigits = 2)

    /**
     * Render [value] for an editable text field: the locale's decimal
     * separator, no grouping, up to [maxFractionDigits] decimals. What it
     * returns always reads back through [parseNumberInput].
     */
    fun formatForInput(
        value: Double,
        locale: Locale = Locale.getDefault(),
        maxFractionDigits: Int = 2,
    ): String {
        val format = DecimalFormat("0", DecimalFormatSymbols.getInstance(locale))
        format.maximumFractionDigits = maxFractionDigits
        return format.format(value)
    }

    /**
     * Parse what the user typed into a number field, in [locale].
     *
     * Strict: the whole text must be one number, with the locale's decimal
     * separator and, if any, correctly placed grouping separators (3-digit
     * groups, or 2-2-3 in en-IN). So in de-DE `10,5` is 10.5, `1.000` is
     * 1000 and `10.5` is rejected instead of silently read as 105.
     * Mid-edit text: `""` and `"-"` give null, `"1."` (trailing separator)
     * gives 1.0.
     *
     * @return the value, or null when the text is not a complete number.
     */
    fun parseNumberInput(
        text: String,
        locale: Locale = Locale.getDefault(),
    ): Double? {
        val symbols = DecimalFormatSymbols.getInstance(locale)
        var input = text.trim()
        // Keyboards type a plain space and "-"; some locales group with a
        // no-break space or use a different minus sign.
        if (symbols.groupingSeparator.isWhitespace()) input = input.replace(' ', symbols.groupingSeparator)
        if (symbols.minusSign != '-') input = input.replace('-', symbols.minusSign)
        if (!hasValidGrouping(input, symbols, locale)) return null
        val position = ParsePosition(0)
        val number = NumberFormat.getNumberInstance(locale).parse(input, position) ?: return null
        // A 400-digit entry would parse to infinity; treat it as invalid.
        return number.toDouble().takeIf { position.index == input.length && it.isFinite() }
    }

    /**
     * Like [parseNumberInput] for fields that take a whole count (months,
     * days, people): returns null for a fraction such as `12.5`.
     */
    fun parseWholeNumberInput(
        text: String,
        locale: Locale = Locale.getDefault(),
    ): Int? =
        parseNumberInput(text, locale)
            ?.takeIf { it == kotlin.math.floor(it) && it in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble() }
            ?.toInt()

    private fun hasValidGrouping(
        input: String,
        symbols: DecimalFormatSymbols,
        locale: Locale,
    ): Boolean {
        val integerPart = input.substringBefore(symbols.decimalSeparator).removePrefix(symbols.minusSign.toString())
        val groups = integerPart.split(symbols.groupingSeparator)
        if (groups.size == 1) return true
        val indian = locale.country == "IN" && locale.language in setOf("en", "hi")
        val middleSize = if (indian) 2 else WESTERN_GROUP
        return groups.first().length in 1..middleSize &&
            groups.drop(1).dropLast(1).all { it.length == middleSize } &&
            groups.last().length == WESTERN_GROUP
    }

    /**
     * Pull the first number out of pasted text and return it in the
     * engine's canonical form (`.` decimal, no grouping), for example
     * `"₹1,234.50"` to `"1234.50"` in en-US or `"1.234,50 €"` to `"1234.50"`
     * in de-DE. Currency symbols and units around it are skipped.
     *
     * @return the canonical number, or null if the text holds none.
     */
    fun extractPastedNumber(
        text: String,
        locale: Locale = Locale.getDefault(),
    ): String? {
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val group = Regex.escape(symbols.groupingSeparator.toString())
        val decimal = Regex.escape(symbols.decimalSeparator.toString())
        val match = Regex("""-?\d(?:[\d ]|$group(?=\d))*(?:$decimal\d+)?""").find(text) ?: return null
        val canonical =
            match.value
                .filterNot { it == ' ' || it == symbols.groupingSeparator }
                .replace(symbols.decimalSeparator, '.')
        return canonical.toBigDecimalOrNull()?.toPlainString()
    }

    private const val INDIAN_LAST_GROUP = 3
    private const val WESTERN_GROUP = 3
}
