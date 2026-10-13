package com.calculator.core.domain.health

import com.calculator.core.domain.health.BmiCalculator.Height
import com.calculator.core.domain.health.BmiCalculator.Weight
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.math.abs

class BmiCalculatorTest {
    @Test
    fun `70kg at 1 point 70m is normal`() {
        val r = BmiCalculator.compute(Height.Cm(170.0), Weight.Kg(70.0))
        assertEquals(24.22, r.bmi, 0.01)
        assertEquals(BmiCalculator.Category.Normal, r.category)
    }

    @Test
    fun `imperial 170lb at 5ft 10in matches metric within 0 point 1`() {
        val metric = BmiCalculator.compute(Height.Cm(177.8), Weight.Kg(77.110703))
        val imperial = BmiCalculator.compute(Height.FeetInches(5, 10.0), Weight.Lb(170.0))
        assertEquals(true, abs(metric.bmi - imperial.bmi) < 0.1) {
            "metric=${metric.bmi} imperial=${imperial.bmi}"
        }
    }

    @Test
    fun `feet and inches with kilograms mixes the units`() {
        // 6 ft 1 in is 185.42 cm.
        val mixed = BmiCalculator.compute(Height.FeetInches(6, 1.0), Weight.Kg(93.0))
        val metric = BmiCalculator.compute(Height.Cm(185.42), Weight.Kg(93.0))
        assertEquals(27.05, mixed.bmi, 0.01)
        assertEquals(metric, mixed)
        assertEquals(BmiCalculator.Category.Overweight, mixed.category)
    }

    @Test
    fun `centimetres with pounds mixes the units`() {
        val mixed = BmiCalculator.compute(Height.Cm(177.8), Weight.Lb(170.0))
        val imperial = BmiCalculator.compute(Height.FeetInches(5, 10.0), Weight.Lb(170.0))
        assertEquals(imperial, mixed)
    }

    @Test
    fun `zero feet and zero inches is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            BmiCalculator.compute(Height.FeetInches(0, 0.0), Weight.Kg(70.0))
        }
    }

    @Test
    fun `below 18 point 5 is underweight`() {
        val r = BmiCalculator.compute(Height.Cm(180.0), Weight.Kg(55.0))
        assertEquals(BmiCalculator.Category.Underweight, r.category)
    }

    @Test
    fun `25 to 30 is overweight`() {
        val r = BmiCalculator.compute(Height.Cm(170.0), Weight.Kg(80.0))
        assertEquals(BmiCalculator.Category.Overweight, r.category)
    }

    @Test
    fun `30 and above is obese`() {
        val r = BmiCalculator.compute(Height.Cm(170.0), Weight.Kg(100.0))
        assertEquals(BmiCalculator.Category.Obese, r.category)
    }
}
