package com.calculator.core.math

import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Unit tests for the scientific subset of [Evaluator]: trig, logarithms,
 * roots, power, and the math constants.
 *
 * Transcendentals route through `Double` then round to DECIMAL64 precision,
 * so direct equality comparisons would fail on the last few digits.
 * Helper [near] compares via `BigDecimal.subtract` and a tolerance.
 */
class ScientificEvaluatorTest {
    private val radEvaluator = Evaluator(angleMode = AngleMode.Radian)
    private val degEvaluator = Evaluator(angleMode = AngleMode.Degree)

    private fun EvaluationResult.success(): BigDecimal {
        assertInstanceOf(EvaluationResult.Success::class.java, this)
        return (this as EvaluationResult.Success).value
    }

    private fun near(actual: BigDecimal, expected: String, tolerance: Double = 1e-10) {
        val diff = (actual - BigDecimal(expected)).abs().toDouble()
        assert(diff < tolerance) { "expected $expected (±$tolerance) but got $actual" }
    }

    // ----- Trigonometry (radian default) -----

    @Test
    fun sineOfZeroIsZero() {
        near(radEvaluator.evaluate("sin(0)").success(), "0")
    }

    @Test
    fun sineOfHalfPiIsOne() {
        near(radEvaluator.evaluate("sin(π÷2)").success(), "1")
    }

    @Test
    fun cosineOfPiIsMinusOne() {
        near(radEvaluator.evaluate("cos(π)").success(), "-1")
    }

    @Test
    fun tangentOfQuarterPiIsOne() {
        near(radEvaluator.evaluate("tan(π÷4)").success(), "1")
    }

    @Test
    fun degreeModeSineOf30IsOneHalf() {
        near(degEvaluator.evaluate("sin(30)").success(), "0.5")
    }

    @Test
    fun degreeModeCosineOf60IsOneHalf() {
        near(degEvaluator.evaluate("cos(60)").success(), "0.5")
    }

    @Test
    fun inverseTrigRoundTrip() {
        near(radEvaluator.evaluate("asin(sin(0.3))").success(), "0.3")
    }

    @Test
    fun cosineOfZeroIsOne() {
        near(radEvaluator.evaluate("cos(0)").success(), "1")
    }

    @Test
    fun arccosineRoundTrip() {
        near(radEvaluator.evaluate("cos(acos(0.5))").success(), "0.5")
    }

    @Test
    fun arctangentOfOneInDegreesIs45() {
        near(degEvaluator.evaluate("atan(1)").success(), "45")
    }

    @Test
    fun tangentOf45DegreesIsOne() {
        near(degEvaluator.evaluate("tan(45)").success(), "1")
    }

    @Test
    fun arcsinOfTwoIsDomainError() {
        // |x| > 1 has no real arcsine.
        val result = radEvaluator.evaluate("asin(2)")
        assertInstanceOf(EvaluationResult.Error.Domain::class.java, result)
    }

    @Test
    fun arccosOfMinusTwoIsDomainError() {
        val result = radEvaluator.evaluate("acos(-2)")
        assertInstanceOf(EvaluationResult.Error.Domain::class.java, result)
    }

    @Test
    fun tangentNearHalfPiIsLarge() {
        // Math.tan(pi/2) doesn't actually return infinity because pi/2 in
        // Double isn't exactly pi/2 - it returns ~1.63e16. The calculator
        // surfaces this as a Success with a very large number rather than
        // a domain error, matching how desk calculators behave near the
        // asymptote. We just assert the magnitude is huge.
        val value = radEvaluator.evaluate("tan(π÷2)").success()
        assert(value.abs().toDouble() > 1e10) { "expected huge tangent, got $value" }
    }

    // ----- Logarithms -----

    @Test
    fun logBase10Of100IsTwo() {
        near(radEvaluator.evaluate("log(100)").success(), "2")
    }

    @Test
    fun naturalLogOfEIsOne() {
        near(radEvaluator.evaluate("ln(e)").success(), "1")
    }

    @Test
    fun logOfNegativeOneIsDomainError() {
        val result = radEvaluator.evaluate("log(-1)")
        assertInstanceOf(EvaluationResult.Error.Domain::class.java, result)
    }

    @Test
    fun sqrtOfNegativeOneIsDomainError() {
        val result = radEvaluator.evaluate("sqrt(-1)")
        assertInstanceOf(EvaluationResult.Error.Domain::class.java, result)
    }

    @Test
    fun logOfZeroIsDomainError() {
        val result = radEvaluator.evaluate("log(0)")
        assertInstanceOf(EvaluationResult.Error.Domain::class.java, result)
    }

    @Test
    fun naturalLogOfZeroIsDomainError() {
        val result = radEvaluator.evaluate("ln(0)")
        assertInstanceOf(EvaluationResult.Error.Domain::class.java, result)
    }

    // ----- Roots & power -----

    @Test
    fun sqrtOfTwoSquaredApproximatesTwo() {
        near(radEvaluator.evaluate("sqrt(2)^2").success(), "2", tolerance = 1e-9)
    }

    @Test
    fun cbrtOf27IsThree() {
        near(radEvaluator.evaluate("cbrt(27)").success(), "3", tolerance = 1e-9)
    }

    @Test
    fun powerIsRightAssociative() {
        // 2^3^2 should be 2^(3^2) = 2^9 = 512, not (2^3)^2 = 64.
        near(radEvaluator.evaluate("2^3^2").success(), "512")
    }

    @Test
    fun powerOf10ToThreeIsThousand() {
        near(radEvaluator.evaluate("10^3").success(), "1000")
    }

    @Test
    fun powerWithFractionalExponent() {
        near(radEvaluator.evaluate("8^(1÷3)").success(), "2", tolerance = 1e-9)
    }

    @Test
    fun negativeBaseWithFractionalExponentIsDomainError() {
        // (-2)^0.5 has no real root.
        val result = radEvaluator.evaluate("(-2)^0.5")
        assertInstanceOf(EvaluationResult.Error.Domain::class.java, result)
    }

    // ----- Constants -----

    @Test
    fun piMatchesMathPi() {
        near(radEvaluator.evaluate("π").success(), Math.PI.toString())
    }

    @Test
    fun eMatchesMathE() {
        near(radEvaluator.evaluate("e").success(), Math.E.toString())
    }

    @Test
    fun expandedPiKeywordIsAccepted() {
        near(radEvaluator.evaluate("pi").success(), Math.PI.toString())
    }

    // ----- Composition -----

    @Test
    fun composedScientificExpressionEvaluates() {
        // sin(30°) + log(100) × π = 0.5 + 2π ≈ 6.7831853...
        val expected = (0.5 + 2 * Math.PI).toString()
        near(degEvaluator.evaluate("sin(30)+log(100)×π").success(), expected, tolerance = 1e-9)
    }

    @Test
    fun unknownIdentifierIsRejected() {
        val result = radEvaluator.evaluate("foo(5)")
        assertInstanceOf(EvaluationResult.Error.UnknownToken::class.java, result)
    }
}
