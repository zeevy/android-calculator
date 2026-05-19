package com.calculator.core.domain.converter

/**
 * The 11 unit-converter categories.
 *
 * Each category has its own [ConversionTable] of [ConverterUnit]s. The
 * order here is the order tabs render in the UI - common categories
 * first (length, area, mass) and information/physics categories last.
 *
 * @property displayName Short human-readable label.
 */
enum class UnitCategory(val displayName: String) {
    Length("Length"),
    Area("Area"),
    Volume("Volume"),
    Mass("Mass"),
    Temperature("Temperature"),
    Speed("Speed"),
    Time("Time"),
    Data("Data"),
    Pressure("Pressure"),
    Energy("Energy"),
    Power("Power"),
}
