package com.calculator.feature.health.bmi

import com.calculator.R
import com.calculator.core.common.input.InputResult
import com.calculator.core.common.input.readNumber
import com.calculator.core.common.input.valueOr
import com.calculator.core.domain.health.BmiCalculator
import com.calculator.core.domain.health.BmiCalculator.Height
import com.calculator.core.domain.health.BmiCalculator.Weight
import com.calculator.core.domain.health.BmiResult
import java.util.Locale

/** Largest inches value the field takes; 12 inches is the next foot. */
private const val MAX_INCHES = 11.99

/**
 * Reads the BMI fields as typed and computes the BMI, or says which
 * field is wrong.
 *
 * @param heightImperial True when height is typed in feet and inches,
 *   false when it is typed in centimetres.
 * @param weightImperial True when [weight] is in pounds, false for kilograms.
 */
@Suppress("LongParameterList")
internal fun bmiInput(
    heightImperial: Boolean,
    heightCm: String,
    heightFeet: String,
    heightInches: String,
    weightImperial: Boolean,
    weight: String,
    locale: Locale = Locale.getDefault(),
): InputResult<BmiResult> {
    val height =
        (if (heightImperial) readFeetInches(heightFeet, heightInches, locale) else readCm(heightCm, locale))
            .valueOr { return it }
    val amount = readNumber(weight, R.string.bmi_weight, locale, min = 0.0, minExclusive = true).valueOr { return it }
    return InputResult.Ok(BmiCalculator.compute(height, if (weightImperial) Weight.Lb(amount) else Weight.Kg(amount)))
}

private fun readCm(
    text: String,
    locale: Locale,
): InputResult<Height> {
    val cm = readNumber(text, R.string.bmi_height, locale, min = 0.0, minExclusive = true).valueOr { return it }
    return InputResult.Ok(Height.Cm(cm))
}

private fun readFeetInches(
    feetText: String,
    inchesText: String,
    locale: Locale,
): InputResult<Height> {
    val feet = readNumber(feetText, R.string.bmi_height_feet, locale, min = 0.0, whole = true).valueOr { return it }
    val inches =
        readNumber(inchesText, R.string.bmi_height_inches, locale, min = 0.0, max = MAX_INCHES).valueOr { return it }
    if (feet == 0.0 && inches == 0.0) return InputResult.Invalid(R.string.bmi_height_feet, R.string.input_error_positive)
    return InputResult.Ok(Height.FeetInches(feet.toInt(), inches))
}
