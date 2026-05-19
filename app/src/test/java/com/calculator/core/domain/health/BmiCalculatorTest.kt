package com.calculator.core.domain.health

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs

class BmiCalculatorTest {
    @Test
    fun `70kg at 1 point 70m is normal`() {
        val r = BmiCalculator.metric(heightCm = 170.0, weightKg = 70.0)
        assertEquals(24.22, r.bmi, 0.01)
        assertEquals(BmiCalculator.Category.Normal, r.category)
    }

    @Test
    fun `imperial 170lb at 5ft 10in matches metric within 0 point 1`() {
        val metric = BmiCalculator.metric(heightCm = 177.8, weightKg = 77.110703)
        val imperial = BmiCalculator.imperial(heightFeet = 5, heightInches = 10.0, weightLb = 170.0)
        assertEquals(true, abs(metric.bmi - imperial.bmi) < 0.1) {
            "metric=${metric.bmi} imperial=${imperial.bmi}"
        }
    }

    @Test
    fun `below 18 point 5 is underweight`() {
        val r = BmiCalculator.metric(heightCm = 180.0, weightKg = 55.0)
        assertEquals(BmiCalculator.Category.Underweight, r.category)
    }

    @Test
    fun `25 to 30 is overweight`() {
        val r = BmiCalculator.metric(heightCm = 170.0, weightKg = 80.0)
        assertEquals(BmiCalculator.Category.Overweight, r.category)
    }

    @Test
    fun `30 and above is obese`() {
        val r = BmiCalculator.metric(heightCm = 170.0, weightKg = 100.0)
        assertEquals(BmiCalculator.Category.Obese, r.category)
    }
}
