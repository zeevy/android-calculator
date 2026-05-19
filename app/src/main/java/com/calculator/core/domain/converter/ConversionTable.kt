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
            ConverterUnit("m", "Meter", 1.0),
            ConverterUnit("km", "Kilometer", 1_000.0),
            ConverterUnit("cm", "Centimeter", 0.01),
            ConverterUnit("mm", "Millimeter", 0.001),
            ConverterUnit("μm", "Micrometer", 1e-6),
            ConverterUnit("nm", "Nanometer", 1e-9),
            ConverterUnit("mi", "Mile", 1609.344),
            ConverterUnit("yd", "Yard", 0.9144),
            ConverterUnit("ft", "Foot", 0.3048),
            ConverterUnit("in", "Inch", 0.0254),
            ConverterUnit("nmi", "Nautical Mile", 1852.0),
        )

    // ---- Area (canonical: square meter) ----
    private val Area =
        listOf(
            ConverterUnit("m²", "Square Meter", 1.0),
            ConverterUnit("km²", "Square Kilometer", 1_000_000.0),
            ConverterUnit("cm²", "Square Centimeter", 1e-4),
            ConverterUnit("mm²", "Square Millimeter", 1e-6),
            ConverterUnit("ha", "Hectare", 10_000.0),
            ConverterUnit("acre", "Acre", 4_046.8564224),
            ConverterUnit("mi²", "Square Mile", 2_589_988.110336),
            ConverterUnit("yd²", "Square Yard", 0.83612736),
            ConverterUnit("ft²", "Square Foot", 0.09290304),
            ConverterUnit("in²", "Square Inch", 6.4516e-4),
        )

    // ---- Volume (canonical: liter) ----
    private val Volume =
        listOf(
            ConverterUnit("L", "Liter", 1.0),
            ConverterUnit("mL", "Milliliter", 0.001),
            ConverterUnit("m³", "Cubic Meter", 1_000.0),
            ConverterUnit("cm³", "Cubic Centimeter", 0.001),
            ConverterUnit("gal US", "US Gallon", 3.785411784),
            ConverterUnit("gal UK", "Imperial Gallon", 4.54609),
            ConverterUnit("qt US", "US Quart", 0.946352946),
            ConverterUnit("pt US", "US Pint", 0.473176473),
            ConverterUnit("cup US", "US Cup", 0.2365882365),
            ConverterUnit("fl oz US", "US Fluid Ounce", 0.0295735296875),
            ConverterUnit("in³", "Cubic Inch", 0.016387064),
            ConverterUnit("ft³", "Cubic Foot", 28.316846592),
        )

    // ---- Mass (canonical: kilogram) ----
    private val Mass =
        listOf(
            ConverterUnit("kg", "Kilogram", 1.0),
            ConverterUnit("g", "Gram", 0.001),
            ConverterUnit("mg", "Milligram", 1e-6),
            ConverterUnit("t", "Metric Ton", 1_000.0),
            ConverterUnit("lb", "Pound", 0.45359237),
            ConverterUnit("oz", "Ounce", 0.028349523125),
            ConverterUnit("st", "Stone", 6.35029318),
            ConverterUnit("ton US", "Short Ton", 907.18474),
            ConverterUnit("ton UK", "Long Ton", 1_016.0469088),
        )

    // ---- Temperature (canonical: Kelvin) ----
    //
    // K = C + 273.15  -> Celsius: factor 1, offset 273.15
    // K = (F + 459.67) × 5/9 -> Fahrenheit: factor 5/9, offset 459.67 × 5/9
    private const val FAHRENHEIT_FACTOR = 5.0 / 9.0
    private val Temperature =
        listOf(
            ConverterUnit("K", "Kelvin", 1.0),
            ConverterUnit("°C", "Celsius", 1.0, toCanonicalOffset = 273.15),
            ConverterUnit("°F", "Fahrenheit", FAHRENHEIT_FACTOR, toCanonicalOffset = 459.67 * FAHRENHEIT_FACTOR),
        )

    // ---- Speed (canonical: meter per second) ----
    private val Speed =
        listOf(
            ConverterUnit("m/s", "Meter per Second", 1.0),
            ConverterUnit("km/h", "Kilometer per Hour", 1.0 / 3.6),
            ConverterUnit("mph", "Mile per Hour", 0.44704),
            ConverterUnit("ft/s", "Foot per Second", 0.3048),
            ConverterUnit("kn", "Knot", 0.514444444444),
        )

    // ---- Time (canonical: second). Month is the 30-day approximation;
    //      year is 365.25 days (Julian year) to round-trip cleanly.
    private val Time =
        listOf(
            ConverterUnit("s", "Second", 1.0),
            ConverterUnit("ms", "Millisecond", 0.001),
            ConverterUnit("μs", "Microsecond", 1e-6),
            ConverterUnit("ns", "Nanosecond", 1e-9),
            ConverterUnit("min", "Minute", 60.0),
            ConverterUnit("h", "Hour", 3_600.0),
            ConverterUnit("d", "Day", 86_400.0),
            ConverterUnit("wk", "Week", 604_800.0),
            ConverterUnit("mo", "Month (30 d)", 2_592_000.0),
            ConverterUnit("yr", "Year (365.25 d)", 31_557_600.0),
        )

    // ---- Data (canonical: byte). Binary multiples (KB = 1024 B). ----
    private val Data =
        listOf(
            ConverterUnit("B", "Byte", 1.0),
            ConverterUnit("KB", "Kilobyte", 1_024.0),
            ConverterUnit("MB", "Megabyte", 1_048_576.0),
            ConverterUnit("GB", "Gigabyte", 1_073_741_824.0),
            ConverterUnit("TB", "Terabyte", 1_099_511_627_776.0),
            ConverterUnit("PB", "Petabyte", 1_125_899_906_842_624.0),
            ConverterUnit("bit", "Bit", 0.125),
        )

    // ---- Pressure (canonical: pascal) ----
    private val Pressure =
        listOf(
            ConverterUnit("Pa", "Pascal", 1.0),
            ConverterUnit("kPa", "Kilopascal", 1_000.0),
            ConverterUnit("MPa", "Megapascal", 1_000_000.0),
            ConverterUnit("bar", "Bar", 100_000.0),
            ConverterUnit("atm", "Atmosphere", 101_325.0),
            ConverterUnit("mmHg", "Millimeter of Mercury", 133.322387415),
            ConverterUnit("psi", "Pound per Square Inch", 6_894.757293168),
            ConverterUnit("Torr", "Torr", 133.322368421),
        )

    // ---- Energy (canonical: joule) ----
    private val Energy =
        listOf(
            ConverterUnit("J", "Joule", 1.0),
            ConverterUnit("kJ", "Kilojoule", 1_000.0),
            ConverterUnit("cal", "Calorie", 4.184),
            ConverterUnit("kcal", "Kilocalorie", 4_184.0),
            ConverterUnit("Wh", "Watt-hour", 3_600.0),
            ConverterUnit("kWh", "Kilowatt-hour", 3_600_000.0),
            ConverterUnit("eV", "Electron Volt", 1.602176634e-19),
            ConverterUnit("BTU", "British Thermal Unit", 1_055.05585262),
        )

    // ---- Power (canonical: watt) ----
    private val Power =
        listOf(
            ConverterUnit("W", "Watt", 1.0),
            ConverterUnit("kW", "Kilowatt", 1_000.0),
            ConverterUnit("MW", "Megawatt", 1_000_000.0),
            ConverterUnit("hp", "Horsepower (mech.)", 745.69987158227022),
            ConverterUnit("BTU/h", "BTU per Hour", 0.29307107),
        )
}
