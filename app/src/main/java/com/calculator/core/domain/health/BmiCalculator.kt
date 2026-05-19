package com.calculator.core.domain.health

import kotlin.math.round

/**
 * Body Mass Index = mass(kg) / height(m)².
 *
 * The calculator accepts inputs in either [UnitSystem.Metric]
 * (kilograms + centimetres) or [UnitSystem.Imperial] (pounds +
 * feet/inches). Output is unit-agnostic BMI (kg/m²) plus a [Category]
 * label using the WHO reference ranges.
 */
object BmiCalculator {
    /**
     * Metric input.
     * @param heightCm Height in centimetres (> 0).
     * @param weightKg Weight in kilograms (> 0).
     */
    fun metric(heightCm: Double, weightKg: Double): BmiResult {
        require(heightCm > 0) { "height must be > 0" }
        require(weightKg > 0) { "weight must be > 0" }
        val heightM = heightCm / 100.0
        return computeFromMetric(heightM, weightKg)
    }

    /**
     * Imperial input.
     * @param heightFeet Whole feet portion of height.
     * @param heightInches Inches portion (0..11.999).
     * @param weightLb Weight in pounds (> 0).
     */
    fun imperial(heightFeet: Int, heightInches: Double, weightLb: Double): BmiResult {
        require(heightFeet >= 0) { "feet must be >= 0" }
        require(heightInches in 0.0..MAX_INCHES_FRACTION) { "inches must be in [0, 12)" }
        require(weightLb > 0) { "weight must be > 0" }
        val heightM = (heightFeet * INCHES_PER_FOOT + heightInches) * INCH_IN_METRES
        val weightKg = weightLb * KG_PER_POUND
        return computeFromMetric(heightM, weightKg)
    }

    private fun computeFromMetric(heightM: Double, weightKg: Double): BmiResult {
        val bmi = weightKg / (heightM * heightM)
        return BmiResult(
            bmi = round(bmi * 100) / 100, // 2 dp - enough for category boundaries
            category = categoryFor(bmi),
        )
    }

    private fun categoryFor(bmi: Double): Category =
        when {
            bmi < UNDERWEIGHT_MAX -> Category.Underweight
            bmi < NORMAL_MAX -> Category.Normal
            bmi < OVERWEIGHT_MAX -> Category.Overweight
            else -> Category.Obese
        }

    enum class Category(val label: String) {
        Underweight("Underweight"),
        Normal("Normal"),
        Overweight("Overweight"),
        Obese("Obese"),
    }

    enum class UnitSystem { Metric, Imperial }

    private const val INCHES_PER_FOOT = 12

    // Largest fractional inches accepted (12.0 would overflow into the
    // next foot, but anything strictly less is valid).
    private const val MAX_INCHES_FRACTION = 11.999
    private const val INCH_IN_METRES = 0.0254
    private const val KG_PER_POUND = 0.45359237

    // WHO adult BMI category boundaries (kg/m^2).
    private const val UNDERWEIGHT_MAX = 18.5
    private const val NORMAL_MAX = 25.0
    private const val OVERWEIGHT_MAX = 30.0
}

data class BmiResult(val bmi: Double, val category: BmiCalculator.Category)
