package com.calculator.core.domain.converter

/**
 * Lookup table: which [ConverterUnit]s belong to each [UnitCategory].
 *
 * The first unit listed per category is the canonical (toCanonicalFactor=1,
 * offset=0); the [Converter] uses it implicitly via the affine map on
 * each unit, so callers don't need to know which one is canonical.
 *
 * Constants are sourced from NIST (https://physics.nist.gov/cuu/Units/).
 * The factors are exact where the underlying SI definitions are exact
 * (mile, foot, inch, pound, etc. are exact by international agreement
 * since 1959).
 *
 * For Data, the binary (KB = 1024 B) convention is used because most
 * users see it that way; if storage-vendor (decimal) units come up they
 * can be added as separate entries (KB SI = 1000 B) later.
 */
object ConversionTable {
    fun unitsFor(category: UnitCategory): List<ConverterUnit> =
        when (category) {
            UnitCategory.Length -> Length
            UnitCategory.Area -> Area
            UnitCategory.Volume -> Volume
            UnitCategory.Mass -> Mass
            UnitCategory.Temperature -> Temperature
            UnitCategory.Speed -> Speed
            UnitCategory.Time -> Time
            UnitCategory.Data -> Data
            UnitCategory.Pressure -> Pressure
            UnitCategory.Energy -> Energy
            UnitCategory.Power -> Power
        }

    // ---- Length (canonical: meter) ----
    private val Length =
        listOf(
            ConverterUnit("m", 1.0),
            ConverterUnit("km", 1_000.0),
            ConverterUnit("cm", 0.01),
            ConverterUnit("mm", 0.001),
            ConverterUnit("μm", 1e-6),
            ConverterUnit("nm", 1e-9),
            ConverterUnit("mi", 1609.344),
            ConverterUnit("yd", 0.9144),
            ConverterUnit("ft", 0.3048),
            ConverterUnit("in", 0.0254),
            ConverterUnit("nmi", 1852.0),
        )

    // ---- Area (canonical: square meter) ----
    private val Area =
        listOf(
            ConverterUnit("m²", 1.0),
            ConverterUnit("km²", 1_000_000.0),
            ConverterUnit("cm²", 1e-4),
            ConverterUnit("mm²", 1e-6),
            ConverterUnit("ha", 10_000.0),
            ConverterUnit("acre", 4_046.8564224),
            ConverterUnit("mi²", 2_589_988.110336),
            ConverterUnit("yd²", 0.83612736),
            ConverterUnit("ft²", 0.09290304),
            ConverterUnit("in²", 6.4516e-4),
        )

    // ---- Volume (canonical: liter) ----
    private val Volume =
        listOf(
            ConverterUnit("L", 1.0),
            ConverterUnit("mL", 0.001),
            ConverterUnit("m³", 1_000.0),
            ConverterUnit("cm³", 0.001),
            ConverterUnit("gal US", 3.785411784),
            ConverterUnit("gal UK", 4.54609),
            ConverterUnit("qt US", 0.946352946),
            ConverterUnit("pt US", 0.473176473),
            ConverterUnit("cup US", 0.2365882365),
            ConverterUnit("fl oz US", 0.0295735296875),
            ConverterUnit("in³", 0.016387064),
            ConverterUnit("ft³", 28.316846592),
        )

    // ---- Mass (canonical: kilogram) ----
    private val Mass =
        listOf(
            ConverterUnit("kg", 1.0),
            ConverterUnit("g", 0.001),
            ConverterUnit("mg", 1e-6),
            ConverterUnit("t", 1_000.0),
            ConverterUnit("lb", 0.45359237),
            ConverterUnit("oz", 0.028349523125),
            ConverterUnit("st", 6.35029318),
            ConverterUnit("ton US", 907.18474),
            ConverterUnit("ton UK", 1_016.0469088),
        )

    // ---- Temperature (canonical: Kelvin) ----
    //
    // K = C + 273.15  -> Celsius: factor 1, offset 273.15
    // K = (F + 459.67) × 5/9 -> Fahrenheit: factor 5/9, offset 459.67 × 5/9
    private const val FAHRENHEIT_FACTOR = 5.0 / 9.0
    private val Temperature =
        listOf(
            ConverterUnit("K", 1.0),
            ConverterUnit("°C", 1.0, toCanonicalOffset = 273.15),
            ConverterUnit("°F", FAHRENHEIT_FACTOR, toCanonicalOffset = 459.67 * FAHRENHEIT_FACTOR),
        )

    // ---- Speed (canonical: meter per second) ----
    private val Speed =
        listOf(
            ConverterUnit("m/s", 1.0),
            ConverterUnit("km/h", 1.0 / 3.6),
            ConverterUnit("mph", 0.44704),
            ConverterUnit("ft/s", 0.3048),
            ConverterUnit("kn", 0.514444444444),
        )

    // ---- Time (canonical: second). Month is the 30-day approximation;
    //      year is 365.25 days (Julian year) to round-trip cleanly.
    private val Time =
        listOf(
            ConverterUnit("s", 1.0),
            ConverterUnit("ms", 0.001),
            ConverterUnit("μs", 1e-6),
            ConverterUnit("ns", 1e-9),
            ConverterUnit("min", 60.0),
            ConverterUnit("h", 3_600.0),
            ConverterUnit("d", 86_400.0),
            ConverterUnit("wk", 604_800.0),
            ConverterUnit("mo", 2_592_000.0),
            ConverterUnit("yr", 31_557_600.0),
        )

    // ---- Data (canonical: byte). Binary multiples (KB = 1024 B). ----
    private val Data =
        listOf(
            ConverterUnit("B", 1.0),
            ConverterUnit("KB", 1_024.0),
            ConverterUnit("MB", 1_048_576.0),
            ConverterUnit("GB", 1_073_741_824.0),
            ConverterUnit("TB", 1_099_511_627_776.0),
            ConverterUnit("PB", 1_125_899_906_842_624.0),
            ConverterUnit("bit", 0.125),
        )

    // ---- Pressure (canonical: pascal) ----
    private val Pressure =
        listOf(
            ConverterUnit("Pa", 1.0),
            ConverterUnit("kPa", 1_000.0),
            ConverterUnit("MPa", 1_000_000.0),
            ConverterUnit("bar", 100_000.0),
            ConverterUnit("atm", 101_325.0),
            ConverterUnit("mmHg", 133.322387415),
            ConverterUnit("psi", 6_894.757293168),
            ConverterUnit("Torr", 133.322368421),
        )

    // ---- Energy (canonical: joule) ----
    private val Energy =
        listOf(
            ConverterUnit("J", 1.0),
            ConverterUnit("kJ", 1_000.0),
            ConverterUnit("cal", 4.184),
            ConverterUnit("kcal", 4_184.0),
            ConverterUnit("Wh", 3_600.0),
            ConverterUnit("kWh", 3_600_000.0),
            ConverterUnit("eV", 1.602176634e-19),
            ConverterUnit("BTU", 1_055.05585262),
        )

    // ---- Power (canonical: watt) ----
    private val Power =
        listOf(
            ConverterUnit("W", 1.0),
            ConverterUnit("kW", 1_000.0),
            ConverterUnit("MW", 1_000_000.0),
            ConverterUnit("hp", 745.69987158227022),
            ConverterUnit("BTU/h", 0.29307107),
        )
}
