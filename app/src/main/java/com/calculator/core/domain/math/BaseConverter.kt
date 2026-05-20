package com.calculator.core.domain.math

/**
 * Bidirectional conversion between binary, octal, decimal, and
 * hexadecimal representations of a non-negative integer.
 *
 * Backed by `Long` so the full 64-bit unsigned range is covered up to
 * `Long.MAX_VALUE` (the signed half is plenty for everyday base
 * conversion; if a use case grows beyond it we can swap in `BigInteger`).
 *
 * Parsing is permissive about case (hex `a` and `A` both work) and
 * ignores underscores so users can group long binary strings as
 * `1010_1100` without breaking the parse. Empty / whitespace-only
 * strings return null - that's a "field is mid-edit" signal at the
 * UI layer, not an error.
 */
object BaseConverter {
    /** Parse a value at the given [radix]; null if [text] is not valid. */
    fun parse(text: String, radix: Int): Long? {
        val cleaned = text.trim().replace("_", "")
        if (cleaned.isEmpty()) return null
        return cleaned.toLongOrNull(radix)
    }

    /** Format [value] in the given [radix], uppercased for hex. */
    fun format(value: Long, radix: Int): String {
        val raw = value.toString(radix)
        return if (radix == HEX) raw.uppercase() else raw
    }

    const val BIN = 2
    const val OCT = 8
    const val DEC = 10
    const val HEX = 16
}
