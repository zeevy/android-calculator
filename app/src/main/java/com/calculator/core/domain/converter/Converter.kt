package com.calculator.core.domain.converter

/**
 * Linear-affine unit conversion.
 *
 * For any two units in the same category we go via the canonical:
 *
 *     canonical = from.value * from.factor + from.offset
 *     result    = (canonical - to.offset) / to.factor
 *
 * Operates in `Double` because all our coefficients fit comfortably in
 * 15 digits of precision and conversions are one-shot (no repeated
 * operations that would compound error like in a calculator REPL).
 * Display formatting at the UI boundary rounds to the user's chosen
 * significant figures.
 */
object Converter {
    fun convert(value: Double, from: ConverterUnit, to: ConverterUnit): Double {
        if (from === to) return value
        val canonical = value * from.toCanonicalFactor + from.toCanonicalOffset
        return (canonical - to.toCanonicalOffset) / to.toCanonicalFactor
    }
}
