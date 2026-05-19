package com.calculator.core.domain.converter

/**
 * A single unit within a [UnitCategory]. All units in a category share a
 * common canonical (reference) unit; the linear-affine pair
 * (factor, offset) defines how this unit maps to that canonical:
 *
 *     canonical = value * toCanonicalFactor + toCanonicalOffset
 *     value     = (canonical - toCanonicalOffset) / toCanonicalFactor
 *
 * The offset is non-zero only for temperature (Celsius / Fahrenheit have
 * non-zero zeros relative to Kelvin); everything else passes
 * `offset = 0` and the math collapses to a simple multiplication.
 *
 * @property symbol Short label (e.g. "m", "kg", "°C") rendered next to
 *   the value field.
 * @property displayName Longer human-readable name shown in the unit
 *   picker (e.g. "Meter", "Kilogram", "Celsius").
 * @property toCanonicalFactor Multiplicative coefficient when converting
 *   to the category's canonical unit.
 * @property toCanonicalOffset Additive coefficient applied after
 *   multiplying. Almost always zero.
 */
data class ConverterUnit(
    val symbol: String,
    val displayName: String,
    val toCanonicalFactor: Double,
    val toCanonicalOffset: Double = 0.0,
)
