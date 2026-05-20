package com.calculator.core.domain.math

/**
 * Number extraction + aggregation for the Smart Paste tool.
 *
 * Given an arbitrary text blob (a chat snippet, a receipt copy, a list
 * of measurements pasted from the web), [extractNumbers] pulls every
 * standalone numeric token out of it and returns them in input order.
 * The format accepted is permissive:
 *
 *  - Optional leading minus (`-3.14`).
 *  - Grouping commas, spaces, or apostrophes inside a digit run
 *    (`1,234`, `1 234`, `1'234`) - dropped before parse.
 *  - Decimal point (period). Commas as decimal separators are NOT
 *    supported - the locale-aware variant would need to know the
 *    user's locale, and the heuristic of "use period for decimals
 *    everywhere" works correctly for the vast majority of pastes from
 *    the web. Users in comma-decimal locales can still paste numbers
 *    one at a time via the basic-calc long-press paste.
 *
 * [aggregate] reduces an already-extracted list to a [SmartPasteSummary]
 * with count, sum, mean, min, and max. Returns null for an empty list
 * (mean / min / max have no meaning without at least one number).
 */
object SmartPaste {
    private val numberRegex = Regex("""-?\d[\d,\s']*(?:\.\d+)?""")

    fun extractNumbers(text: String): List<Double> =
        numberRegex
            .findAll(text)
            .mapNotNull { match ->
                match.value
                    .replace(",", "")
                    .replace("'", "")
                    .replace(Regex("""\s+"""), "")
                    .toDoubleOrNull()
            }
            .toList()

    fun aggregate(numbers: List<Double>): SmartPasteSummary? {
        if (numbers.isEmpty()) return null
        val sum = numbers.sum()
        return SmartPasteSummary(
            count = numbers.size,
            sum = sum,
            mean = sum / numbers.size,
            min = numbers.min(),
            max = numbers.max(),
        )
    }
}

data class SmartPasteSummary(
    val count: Int,
    val sum: Double,
    val mean: Double,
    val min: Double,
    val max: Double,
)
