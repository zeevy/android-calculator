package com.calculator.core.domain.converter

/**
 * The 11 unit-converter categories.
 *
 * Each category has its own [ConversionTable] of [ConverterUnit]s. The
 * order here is the order tabs render in the UI - common categories
 * first (length, area, mass) and information/physics categories last.
 *
 * The UI maps each one to a translated label.
 */
enum class UnitCategory {
    Length,
    Area,
    Volume,
    Mass,
    Temperature,
    Speed,
    Time,
    Data,
    Pressure,
    Energy,
    Power,
}
