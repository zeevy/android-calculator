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
