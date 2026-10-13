package com.calculator.feature.health.bmi

import com.calculator.R
import com.calculator.core.common.input.InputResult
import com.calculator.core.domain.health.BmiCalculator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.Locale

/** BMI fields as typed, to a BMI or a per-field error. */
class BmiInputTest {
    @Suppress("LongParameterList")
    private fun input(
        heightImperial: Boolean = false,
        heightCm: String = "170",
        heightFeet: String = "5",
        heightInches: String = "10",
        weightImperial: Boolean = false,
        weight: String = "70",
    ) = bmiInput(heightImperial, heightCm, heightFeet, heightInches, weightImperial, weight, Locale.US)

    private fun InputResult<*>.invalid() = this as InputResult.Invalid

    @Test
    fun `centimetres and kilograms give the BMI`() {
        val ok = input() as InputResult.Ok
        assertEquals(24.22, ok.value.bmi, 0.01)
        assertEquals(BmiCalculator.Category.Normal, ok.value.category)
    }

    @Test
    fun `feet, inches and pounds give the BMI`() {
        val ok = input(heightImperial = true, weightImperial = true, weight = "170") as InputResult.Ok
        assertEquals(24.39, ok.value.bmi, 0.01)
    }

    @Test
    fun `feet and inches mix with kilograms`() {
        val ok = input(heightImperial = true, heightFeet = "6", heightInches = "1", weight = "93") as InputResult.Ok
        assertEquals(27.05, ok.value.bmi, 0.01)
        assertEquals(BmiCalculator.Category.Overweight, ok.value.category)
    }

    @Test
    fun `only the fields of the chosen units are read`() {
        assertEquals(input(), input(heightFeet = "", heightInches = "15"))
        assertEquals(input(heightImperial = true), input(heightImperial = true, heightCm = ""))
    }

    @Test
    fun `empty fields are named`() {
        assertEquals(R.string.bmi_height, input(heightCm = "").invalid().field)
        assertEquals(R.string.input_error_required, input(heightCm = "").invalid().message)
        assertEquals(R.string.bmi_height_feet, input(heightImperial = true, heightFeet = "").invalid().field)
        assertEquals(R.string.bmi_height_inches, input(heightImperial = true, heightInches = " ").invalid().field)
        assertEquals(R.string.bmi_weight, input(weight = "").invalid().field)
    }

    @ParameterizedTest
    @ValueSource(strings = ["12", "15", "-3", "11.995"])
    fun `inches outside 0 to 12 are rejected on the inches field`(inches: String) {
        val error = input(heightImperial = true, heightInches = inches).invalid()
        assertEquals(R.string.bmi_height_inches, error.field)
        assertEquals(R.string.input_error_range, error.message)
        assertEquals(listOf("0", "11.99"), error.args)
    }

    @Test
    fun `feet must be a whole number of at least 0`() {
        assertEquals(R.string.input_error_whole, input(heightImperial = true, heightFeet = "5.5").invalid().message)
        val negative = input(heightImperial = true, heightFeet = "-1").invalid()
        assertEquals(R.string.bmi_height_feet, negative.field)
        assertEquals(R.string.input_error_negative, negative.message)
    }

    @Test
    fun `zero height is rejected`() {
        assertEquals(R.string.input_error_positive, input(heightCm = "0").invalid().message)
        val zeroFeetInches = input(heightImperial = true, heightFeet = "0", heightInches = "0").invalid()
        assertEquals(R.string.bmi_height_feet, zeroFeetInches.field)
        assertEquals(R.string.input_error_positive, zeroFeetInches.message)
    }

    @Test
    fun `zero weight is rejected on the weight field`() {
        val error = input(weight = "0").invalid()
        assertEquals(R.string.bmi_weight, error.field)
        assertEquals(R.string.input_error_positive, error.message)
        assertEquals(R.string.bmi_weight, input(weightImperial = true, weight = "0").invalid().field)
    }
}
