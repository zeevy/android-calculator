package com.calculator.core.domain.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * Conversion-correctness tests, one suite per category plus a
 * round-trip suite that exercises every pair of units within each
 * category.
 */
class ConverterTest {
    private fun near(actual: Double, expected: Double, tolerance: Double = 1e-9) {
        assert(abs(actual - expected) < tolerance) {
            "expected $expected (±$tolerance) but got $actual (diff ${abs(actual - expected)})"
        }
    }

    private fun unitOf(category: UnitCategory, symbol: String): ConverterUnit =
        ConversionTable.unitsFor(category).single { it.symbol == symbol }

    // ----- Length -----

    @Test
    fun `1 km equals 1000 m`() {
        val km = unitOf(UnitCategory.Length, "km")
        val m = unitOf(UnitCategory.Length, "m")
        near(Converter.convert(1.0, km, m), 1_000.0)
    }

    @Test
    fun `1 mile equals 1609 point 344 m`() {
        val mi = unitOf(UnitCategory.Length, "mi")
        val m = unitOf(UnitCategory.Length, "m")
        near(Converter.convert(1.0, mi, m), 1609.344)
    }

    @Test
    fun `1 inch equals 2 point 54 cm`() {
        val inch = unitOf(UnitCategory.Length, "in")
        val cm = unitOf(UnitCategory.Length, "cm")
        near(Converter.convert(1.0, inch, cm), 2.54, tolerance = 1e-12)
    }

    // ----- Mass -----

    @Test
    fun `1 kg approximates 2 point 20462 lb`() {
        val kg = unitOf(UnitCategory.Mass, "kg")
        val lb = unitOf(UnitCategory.Mass, "lb")
        near(Converter.convert(1.0, kg, lb), 2.20462262, tolerance = 1e-5)
    }

    @Test
    fun `1 stone equals 14 pounds`() {
        val st = unitOf(UnitCategory.Mass, "st")
        val lb = unitOf(UnitCategory.Mass, "lb")
        near(Converter.convert(1.0, st, lb), 14.0, tolerance = 1e-9)
    }

    // ----- Temperature (the only category with a non-zero offset) -----

    @Test
    fun `0 C equals 32 F`() {
        val c = unitOf(UnitCategory.Temperature, "°C")
        val f = unitOf(UnitCategory.Temperature, "°F")
        near(Converter.convert(0.0, c, f), 32.0, tolerance = 1e-9)
    }

    @Test
    fun `100 C equals 212 F`() {
        val c = unitOf(UnitCategory.Temperature, "°C")
        val f = unitOf(UnitCategory.Temperature, "°F")
        near(Converter.convert(100.0, c, f), 212.0, tolerance = 1e-9)
    }

    @Test
    fun `0 C equals 273 point 15 K`() {
        val c = unitOf(UnitCategory.Temperature, "°C")
        val k = unitOf(UnitCategory.Temperature, "K")
        near(Converter.convert(0.0, c, k), 273.15, tolerance = 1e-9)
    }

    @Test
    fun `-40 C equals -40 F`() {
        val c = unitOf(UnitCategory.Temperature, "°C")
        val f = unitOf(UnitCategory.Temperature, "°F")
        near(Converter.convert(-40.0, c, f), -40.0, tolerance = 1e-9)
    }

    // ----- Volume -----

    @Test
    fun `1 L equals 1000 mL`() {
        val l = unitOf(UnitCategory.Volume, "L")
        val ml = unitOf(UnitCategory.Volume, "mL")
        near(Converter.convert(1.0, l, ml), 1_000.0)
    }

    @Test
    fun `1 US gallon equals 3 point 78541 L`() {
        val gal = unitOf(UnitCategory.Volume, "gal US")
        val l = unitOf(UnitCategory.Volume, "L")
        near(Converter.convert(1.0, gal, l), 3.785411784, tolerance = 1e-9)
    }

    // ----- Data (binary multiples) -----

    @Test
    fun `1 GB equals 1024 MB`() {
        val gb = unitOf(UnitCategory.Data, "GB")
        val mb = unitOf(UnitCategory.Data, "MB")
        near(Converter.convert(1.0, gb, mb), 1024.0)
    }

    @Test
    fun `1 byte equals 8 bits`() {
        val byte = unitOf(UnitCategory.Data, "B")
        val bit = unitOf(UnitCategory.Data, "bit")
        near(Converter.convert(1.0, byte, bit), 8.0, tolerance = 1e-12)
    }

    // ----- Time -----

    @Test
    fun `1 hour equals 3600 seconds`() {
        val h = unitOf(UnitCategory.Time, "h")
        val s = unitOf(UnitCategory.Time, "s")
        near(Converter.convert(1.0, h, s), 3_600.0)
    }

    @Test
    fun `1 day equals 24 hours`() {
        val d = unitOf(UnitCategory.Time, "d")
        val h = unitOf(UnitCategory.Time, "h")
        near(Converter.convert(1.0, d, h), 24.0, tolerance = 1e-9)
    }

    // ----- Pressure -----

    @Test
    fun `1 atm equals 101325 Pa`() {
        val atm = unitOf(UnitCategory.Pressure, "atm")
        val pa = unitOf(UnitCategory.Pressure, "Pa")
        near(Converter.convert(1.0, atm, pa), 101_325.0, tolerance = 1e-6)
    }

    // ----- Energy -----

    @Test
    fun `1 kcal equals 4184 J`() {
        val kcal = unitOf(UnitCategory.Energy, "kcal")
        val j = unitOf(UnitCategory.Energy, "J")
        near(Converter.convert(1.0, kcal, j), 4_184.0, tolerance = 1e-9)
    }

    // ----- Power -----

    @Test
    fun `1 kW equals 1000 W`() {
        val kw = unitOf(UnitCategory.Power, "kW")
        val w = unitOf(UnitCategory.Power, "W")
        near(Converter.convert(1.0, kw, w), 1_000.0)
    }

    // ----- Speed -----

    @Test
    fun `60 mph approximates 96 point 56 kmh`() {
        val mph = unitOf(UnitCategory.Speed, "mph")
        val kmh = unitOf(UnitCategory.Speed, "km/h")
        near(Converter.convert(60.0, mph, kmh), 96.56064, tolerance = 1e-6)
    }

    // ----- Area -----

    @Test
    fun `1 hectare equals 10000 square meters`() {
        val ha = unitOf(UnitCategory.Area, "ha")
        val m2 = unitOf(UnitCategory.Area, "m²")
        near(Converter.convert(1.0, ha, m2), 10_000.0)
    }

    // ----- Round-trip across every pair within every category -----

    @Test
    fun `round-trip every pair returns within tolerance`() {
        val value = 7.5
        UnitCategory.entries.forEach { category ->
            val units = ConversionTable.unitsFor(category)
            for (from in units) {
                for (to in units) {
                    val forward = Converter.convert(value, from, to)
                    val back = Converter.convert(forward, to, from)
                    val rel = if (value == 0.0) abs(back) else abs(back - value) / abs(value)
                    assert(rel < 1e-9) {
                        "round-trip failed: $value ${from.symbol} -> ${to.symbol} -> ${from.symbol} = $back (rel err $rel)"
                    }
                }
            }
        }
    }

    @Test
    fun `every category exposes at least two units`() {
        UnitCategory.entries.forEach { category ->
            assertEquals(
                true,
                ConversionTable.unitsFor(category).size >= 2,
                "category ${category.name} has fewer than 2 units",
            )
        }
    }

    @Test
    fun `every unit has a non-zero conversion factor`() {
        UnitCategory.entries.forEach { category ->
            ConversionTable.unitsFor(category).forEach { unit ->
                assert(unit.toCanonicalFactor != 0.0) {
                    "${category.name} unit ${unit.symbol} has zero factor"
                }
            }
        }
    }
}
