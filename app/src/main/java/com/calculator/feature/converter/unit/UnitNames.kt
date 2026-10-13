package com.calculator.feature.converter.unit

import androidx.annotation.StringRes
import com.calculator.R
import com.calculator.core.domain.converter.ConverterUnit
import com.calculator.core.domain.converter.UnitCategory

/** Display name of this category, translated. */
@StringRes
internal fun UnitCategory.labelRes(): Int =
    when (this) {
        UnitCategory.Length -> R.string.unit_category_length
        UnitCategory.Area -> R.string.unit_category_area
        UnitCategory.Volume -> R.string.unit_category_volume
        UnitCategory.Mass -> R.string.unit_category_mass
        UnitCategory.Temperature -> R.string.unit_category_temperature
        UnitCategory.Speed -> R.string.unit_category_speed
        UnitCategory.Time -> R.string.unit_category_time
        UnitCategory.Data -> R.string.unit_category_data
        UnitCategory.Pressure -> R.string.unit_category_pressure
        UnitCategory.Energy -> R.string.unit_category_energy
        UnitCategory.Power -> R.string.unit_category_power
    }

/** Display name of this unit, translated. The domain keeps only the symbol. */
@StringRes
internal fun ConverterUnit.labelRes(): Int = UnitNames.getValue(symbol)

private val UnitNames: Map<String, Int> =
    mapOf(
        "m" to R.string.unit_meter,
        "km" to R.string.unit_kilometer,
        "cm" to R.string.unit_centimeter,
        "mm" to R.string.unit_millimeter,
        "μm" to R.string.unit_micrometer,
        "nm" to R.string.unit_nanometer,
        "mi" to R.string.unit_mile,
        "yd" to R.string.unit_yard,
        "ft" to R.string.unit_foot,
        "in" to R.string.unit_inch,
        "nmi" to R.string.unit_nautical_mile,
        "m²" to R.string.unit_square_meter,
        "km²" to R.string.unit_square_kilometer,
        "cm²" to R.string.unit_square_centimeter,
        "mm²" to R.string.unit_square_millimeter,
        "ha" to R.string.unit_hectare,
        "acre" to R.string.unit_acre,
        "mi²" to R.string.unit_square_mile,
        "yd²" to R.string.unit_square_yard,
        "ft²" to R.string.unit_square_foot,
        "in²" to R.string.unit_square_inch,
        "L" to R.string.unit_liter,
        "mL" to R.string.unit_milliliter,
        "m³" to R.string.unit_cubic_meter,
        "cm³" to R.string.unit_cubic_centimeter,
        "gal US" to R.string.unit_us_gallon,
        "gal UK" to R.string.unit_imperial_gallon,
        "qt US" to R.string.unit_us_quart,
        "pt US" to R.string.unit_us_pint,
        "cup US" to R.string.unit_us_cup,
        "fl oz US" to R.string.unit_us_fluid_ounce,
        "in³" to R.string.unit_cubic_inch,
        "ft³" to R.string.unit_cubic_foot,
        "kg" to R.string.unit_kilogram,
        "g" to R.string.unit_gram,
        "mg" to R.string.unit_milligram,
        "t" to R.string.unit_metric_ton,
        "lb" to R.string.unit_pound,
        "oz" to R.string.unit_ounce,
        "st" to R.string.unit_stone,
        "ton US" to R.string.unit_short_ton,
        "ton UK" to R.string.unit_long_ton,
        "K" to R.string.unit_kelvin,
        "°C" to R.string.unit_celsius,
        "°F" to R.string.unit_fahrenheit,
        "m/s" to R.string.unit_meter_per_second,
        "km/h" to R.string.unit_kilometer_per_hour,
        "mph" to R.string.unit_mile_per_hour,
        "ft/s" to R.string.unit_foot_per_second,
        "kn" to R.string.unit_knot,
        "s" to R.string.unit_second,
        "ms" to R.string.unit_millisecond,
        "μs" to R.string.unit_microsecond,
        "ns" to R.string.unit_nanosecond,
        "min" to R.string.unit_minute,
        "h" to R.string.unit_hour,
        "d" to R.string.unit_day,
        "wk" to R.string.unit_week,
        "mo" to R.string.unit_month,
        "yr" to R.string.unit_year,
        "B" to R.string.unit_byte,
        "KB" to R.string.unit_kilobyte,
        "MB" to R.string.unit_megabyte,
        "GB" to R.string.unit_gigabyte,
        "TB" to R.string.unit_terabyte,
        "PB" to R.string.unit_petabyte,
        "bit" to R.string.unit_bit,
        "Pa" to R.string.unit_pascal,
        "kPa" to R.string.unit_kilopascal,
        "MPa" to R.string.unit_megapascal,
        "bar" to R.string.unit_bar,
        "atm" to R.string.unit_atmosphere,
        "mmHg" to R.string.unit_millimeter_of_mercury,
        "psi" to R.string.unit_pound_per_square_inch,
        "Torr" to R.string.unit_torr,
        "J" to R.string.unit_joule,
        "kJ" to R.string.unit_kilojoule,
        "cal" to R.string.unit_calorie,
        "kcal" to R.string.unit_kilocalorie,
        "Wh" to R.string.unit_watt_hour,
        "kWh" to R.string.unit_kilowatt_hour,
        "eV" to R.string.unit_electron_volt,
        "BTU" to R.string.unit_british_thermal_unit,
        "W" to R.string.unit_watt,
        "kW" to R.string.unit_kilowatt,
        "MW" to R.string.unit_megawatt,
        "hp" to R.string.unit_horsepower,
        "BTU/h" to R.string.unit_btu_per_hour,
    )
