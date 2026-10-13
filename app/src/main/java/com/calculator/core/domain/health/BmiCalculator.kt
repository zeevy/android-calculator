package com.calculator.core.domain.health

import kotlin.math.round

/**
 * Body Mass Index = mass(kg) / height(m)².
 *
 * Height and weight each come in their own unit ([Height], [Weight]),
 * so feet and inches can be mixed with kilograms. Output is BMI in
 * kg/m² plus a [Category] label using the WHO reference ranges.
 */
object BmiCalculator {
    /** Height in one of the units the BMI screen offers. */
    sealed interface Height {
        /** Height in centimetres. */
        data class Cm(
            val cm: Double,
        ) : Height

        /** Height as whole [feet] plus [inches] from 0 up to, but not including, 12. */
        data class FeetInches(
            val feet: Int,
            val inches: Double,
        ) : Height
    }

    /** Weight in one of the units the BMI screen offers. */
    sealed interface Weight {
        /** Weight in kilograms. */
        data class Kg(
            val kg: Double,
        ) : Weight

        /** Weight in pounds. */
        data class Lb(
            val lb: Double,
        ) : Weight
    }

    /**
     * BMI for [height] and [weight].
     *
     * @throws IllegalArgumentException when the height or the weight is
     *   not more than 0, or the feet or inches are out of range.
     */
    fun compute(height: Height, weight: Weight): BmiResult {
        val heightM =
            when (height) {
                is Height.Cm -> height.cm / 100.0
                is Height.FeetInches -> {
                    require(height.feet >= 0) { "feet must be >= 0" }
                    require(height.inches >= 0.0 && height.inches < INCHES_PER_FOOT) { "inches must be in [0, 12)" }
                    // Double maths, so a very large feet value cannot overflow Int.
                    (height.feet.toDouble() * INCHES_PER_FOOT + height.inches) * INCH_IN_METRES
                }
            }
        val weightKg =
            when (weight) {
                is Weight.Kg -> weight.kg
                is Weight.Lb -> weight.lb * KG_PER_POUND
            }
        require(heightM > 0) { "height must be > 0" }
        require(weightKg > 0) { "weight must be > 0" }
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

    private const val INCHES_PER_FOOT = 12

    // Exact conversion factors (NIST).
    private const val INCH_IN_METRES = 0.0254
    private const val KG_PER_POUND = 0.45359237

    // WHO adult BMI category boundaries (kg/m^2).
    private const val UNDERWEIGHT_MAX = 18.5
    private const val NORMAL_MAX = 25.0
    private const val OVERWEIGHT_MAX = 30.0
}

data class BmiResult(val bmi: Double, val category: BmiCalculator.Category)
