package com.calculator.core.domain.health

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Golden catalogue for [BmiCalculator] across metric and imperial
 * inputs and every WHO category boundary.
 */
class BmiCalculatorGoldenTest {
    @DisplayName("metric: BMI = weightKg / heightM² (within 2dp rounding)")
    @ParameterizedTest(name = "{0}cm + {1}kg -> {2} ({3})")
    @CsvSource(
        // Each row checks a different WHO category
        "180,     50,     15.43,  Underweight",
        "180,     55,     16.98,  Underweight",
        "180,     60,     18.52,  Normal",
        "180,     70,     21.60,  Normal",
        "180,     75,     23.15,  Normal",
        "180,     80,     24.69,  Normal",
        "180,     82,     25.31,  Overweight",
        "180,     90,     27.78,  Overweight",
        "180,     97,     29.94,  Overweight",
        "180,     100,    30.86,  Obese",
        "180,     120,    37.04,  Obese",
        // Different heights
        "160,     60,     23.44,  Normal",
        "170,     70,     24.22,  Normal",
        "190,     85,     23.55,  Normal",
        "150,     45,     20.00,  Normal",
        // Category boundaries (just below / just above the line)
        "170,     53.45,  18.49,  Underweight",
        "170,     53.5,   18.51,  Normal",
        "170,     72.25,  25.00,  Overweight",
        "170,     86.7,   30.00,  Obese",
    )
    fun metricBmiAndCategory(
        heightCm: Double,
        weightKg: Double,
        expectedBmi: Double,
        expectedCategory: BmiCalculator.Category,
    ) {
        val r = BmiCalculator.metric(heightCm = heightCm, weightKg = weightKg)
        assertEquals(expectedBmi, r.bmi, 0.02)
        assertEquals(expectedCategory, r.category)
    }

    @DisplayName("imperial: same formula via lb + ft/in conversion")
    @ParameterizedTest(name = "{0}'{1}\" {2}lb -> {3}")
    @CsvSource(
        // Common adult ranges
        "5,   10,   150,    21.52",
        "5,   10,   180,    25.82",
        "5,   10,   200,    28.69",
        "5,   10,   220,    31.56",
        "5,   6,    140,    22.59",
        "5,   0,    100,    19.53",
        "6,   0,    180,    24.41",
        "6,   2,    200,    25.68",
    )
    fun imperialBmi(feet: Int, inches: Double, weightLb: Double, expectedBmi: Double) {
        val r = BmiCalculator.imperial(heightFeet = feet, heightInches = inches, weightLb = weightLb)
        assertEquals(expectedBmi, r.bmi, 0.05)
    }

    @DisplayName("category boundaries are < (strict), not <=")
    @org.junit.jupiter.api.Test
    fun boundariesAreStrictLessThan() {
        // BMI exactly at 18.5 = Normal (Underweight is "< 18.5")
        // To land on exactly 18.5, choose h=2.0m and w=74.0kg → 74/4 = 18.5
        val onUnderweightBoundary = BmiCalculator.metric(heightCm = 200.0, weightKg = 74.0)
        assertEquals(BmiCalculator.Category.Normal, onUnderweightBoundary.category)

        // BMI exactly at 25.0 = Overweight (Normal is "< 25")
        val onNormalBoundary = BmiCalculator.metric(heightCm = 200.0, weightKg = 100.0)
        assertEquals(BmiCalculator.Category.Overweight, onNormalBoundary.category)

        // BMI exactly at 30.0 = Obese (Overweight is "< 30")
        val onOverweightBoundary = BmiCalculator.metric(heightCm = 200.0, weightKg = 120.0)
        assertEquals(BmiCalculator.Category.Obese, onOverweightBoundary.category)
    }

    @DisplayName("rejects invalid inputs")
    @org.junit.jupiter.api.Test
    fun rejectsInvalidInputs() {
        // Non-positive height / weight
        assertThrows(IllegalArgumentException::class.java) { BmiCalculator.metric(0.0, 70.0) }
        assertThrows(IllegalArgumentException::class.java) { BmiCalculator.metric(-1.0, 70.0) }
        assertThrows(IllegalArgumentException::class.java) { BmiCalculator.metric(180.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { BmiCalculator.metric(180.0, -1.0) }
        // Imperial: out-of-range inches
        assertThrows(IllegalArgumentException::class.java) {
            BmiCalculator.imperial(heightFeet = 5, heightInches = 12.0, weightLb = 150.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BmiCalculator.imperial(heightFeet = 5, heightInches = -0.5, weightLb = 150.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BmiCalculator.imperial(heightFeet = -1, heightInches = 0.0, weightLb = 150.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BmiCalculator.imperial(heightFeet = 5, heightInches = 10.0, weightLb = -1.0)
        }
    }
}
