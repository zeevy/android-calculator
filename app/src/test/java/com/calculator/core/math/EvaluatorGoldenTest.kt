package com.calculator.core.math

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

/**
 * Golden-value test for [Evaluator]: a wide catalogue of (expression,
 * expected result) pairs run through the engine in a single fast JVM
 * pass.
 *
 * Purpose
 * -------
 * The targeted tests in [EvaluatorTest] and [ScientificEvaluatorTest]
 * are narrative - each one exercises a single property. This file is
 * the opposite: a flat catalogue that exists to catch regressions
 * across the whole surface of the engine at once. When a math bug
 * slips through (e.g. sin(π) returning 1.22e-16 instead of 0, or
 * 33...3 + 33...3 silently rounding to 16 sig figs), add a row here
 * so the same shape of bug can never come back.
 *
 * Conventions
 * -----------
 * - "Success" rows use [successCases] and compare values via
 *   [BigDecimal.compareTo] so a `1` result matches a `1.0` expectation.
 * - "Near" rows use [transcendentalCases] and assert |actual - expected|
 *   is below a tolerance, because trig / log / sqrt route through Double
 *   and the last digit is non-deterministic.
 * - "Error" rows use [errorCases] and assert the right [EvaluationResult.Error]
 *   subtype is returned.
 *
 * Why CsvSource not @MethodSource: a one-line addition is the lowest-
 * friction way to expand this catalogue, and the JUnit5 test names
 * already include the row's column 0 (the expression) so failures are
 * self-describing.
 */
class EvaluatorGoldenTest {
    private val rad = Evaluator(angleMode = AngleMode.Radian)
    private val deg = Evaluator(angleMode = AngleMode.Degree)

    // ------------------------------------------------------------------
    // Exact-success cases: the engine must return precisely this value.
    // ------------------------------------------------------------------

    @DisplayName("exact arithmetic")
    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
        // basic
        "'0',                       0",
        "'1+1',                     2",
        "'2-3',                     -1",
        "'2×3',                     6",
        "'6÷2',                     3",
        "'-5',                      -5",
        "'-5+3',                    -2",
        // precedence
        "'1+2×3',                   7",
        "'(1+2)×3',                 9",
        "'2+3×4-5',                 9",
        "'10-2×3',                  4",
        "'20÷4÷5',                  1",
        // nested parens
        "'((2+3)×4)',               20",
        "'2×(3+(4-1))',             12",
        "'((1+2)+(3+4))',           10",
        // decimals (exact terminating)
        "'1.5+2.25',                3.75",
        "'0.1+0.2',                 0.3",
        "'1.5×4',                   6",
        "'0.5×0.5',                 0.25",
        "'10×0.1',                  1",
        // unary minus folded into a literal
        "'-3+5',                    2",
        "'-3×4',                    -12",
        "'-2×-3',                   6",
        // large-integer + - × must be EXACT (no DECIMAL64 rounding)
        "'33333333333333333333+33333333333333333333',   66666666666666666666",
        "'99999999999999999999-1',                       99999999999999999998",
        "'99999999999999999999×2',                       199999999999999999998",
        "'12345678901234567890×0',                       0",
        // a multiplication that would round under DECIMAL64
        "'9999999999×9999999999',                        99999999980000000001",
        // identity / zero
        "'0×123456789',             0",
        "'123456789×1',             123456789",
        "'0+0',                     0",
        "'1-1',                     0",
        // mixed operator chains
        "'1+2-3+4-5',               -1",
        "'2×3÷6',                   1",
        "'7÷2×2',                   7",
        // negative results
        "'5-10',                    -5",
        "'-(2+3)',                  -5",
        // factorial
        "'0!',                      1",
        "'1!',                      1",
        "'5!',                      120",
        "'10!',                     3628800",
        "'13!',                     6227020800",
        // sign flip composes with factorial: -(5!) NOT (-5)!
        "'-5!',                     -120",
        // power: integer cases
        "'2^0',                     1",
        "'2^10',                    1024",
        "'10^3',                    1000",
        // squared / cubed via the engine's symbol form
        "'3^2',                     9",
        "'4^3',                     64",
    )
    fun exactArithmetic(expression: String, expected: String) {
        val value = rad.evaluate(expression).expectSuccess()
        assertEquals(
            0,
            value.compareTo(BigDecimal(expected)),
            "expected $expected but got $value",
        )
    }

    // ------------------------------------------------------------------
    // Transcendental cases: results compared within a tolerance because
    // the implementations route through Double.
    // ------------------------------------------------------------------

    @DisplayName("transcendentals in radians")
    @ParameterizedTest(name = "{0} ≈ {1}")
    @CsvSource(
        // exact zeros - the recent zero-snap fix must keep these clean
        "'sin(0)',                  0",
        "'sin(π)',                  0",
        "'cos(π÷2)',                0",
        "'tan(0)',                  0",
        "'tan(π)',                  0",
        "'asin(0)',                 0",
        "'atan(0)',                 0",
        "'log(1)',                  0",
        "'ln(1)',                   0",
        // exact ones
        "'cos(0)',                  1",
        "'sin(π÷2)',                1",
        "'log(10)',                 1",
        "'ln(e)',                   1",
        // exact integer outputs for common angles
        "'log(100)',                2",
        "'log(1000)',               3",
        "'log(0.01)',               -2",
        "'2^0.5×2^0.5',             2",
        // identities (within tolerance)
        "'sin(π÷6)',                0.5",
        "'cos(π÷3)',                0.5",
        "'tan(π÷4)',                1",
        "'sin(π÷4)',                0.70710678118654",
        "'asin(1)',                 1.5707963267948",
        "'acos(0)',                 1.5707963267948",
        "'acos(1)',                 0",
        "'atan(1)',                 0.78539816339744",
        // roots
        "'sqrt(0)',                 0",
        "'sqrt(1)',                 1",
        "'sqrt(4)',                 2",
        "'sqrt(9)',                 3",
        "'sqrt(2)',                 1.41421356237309",
        "'cbrt(0)',                 0",
        "'cbrt(8)',                 2",
        "'cbrt(27)',                3",
        "'cbrt(-8)',                -2",
        // exp/log identities
        "'ln(e^2)',                 2",
        // constants
        "'π',                       3.14159265358979",
        "'e',                       2.71828182845904",
        // power with fractional exponent
        "'8^(1÷3)',                 2",
        "'27^(1÷3)',                3",
    )
    fun transcendentalsRadians(expression: String, expected: String) {
        val value = rad.evaluate(expression).expectSuccess()
        assertNear(value, expected)
    }

    @DisplayName("transcendentals in degrees")
    @ParameterizedTest(name = "deg {0} ≈ {1}")
    @CsvSource(
        // degree-mode favourites - exact rationals
        "'sin(0)',                  0",
        "'sin(30)',                 0.5",
        "'sin(90)',                 1",
        "'sin(180)',                0",
        "'cos(0)',                  1",
        "'cos(60)',                 0.5",
        "'cos(90)',                 0",
        "'cos(180)',                -1",
        "'tan(0)',                  0",
        "'tan(45)',                 1",
        "'tan(180)',                0",
        // inverse, returning degrees
        "'asin(0)',                 0",
        "'asin(0.5)',               30",
        "'asin(1)',                 90",
        "'acos(0)',                 90",
        "'acos(1)',                 0",
        "'atan(1)',                 45",
    )
    fun transcendentalsDegrees(expression: String, expected: String) {
        val value = deg.evaluate(expression).expectSuccess()
        assertNear(value, expected)
    }

    // ------------------------------------------------------------------
    // Error cases: every row asserts a specific [Error] subtype is
    // returned (never thrown, per the engine's documented contract).
    // ------------------------------------------------------------------

    @DisplayName("error paths")
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        // division by zero
        "'1÷0',                     DivisionByZero",
        "'0÷0',                     DivisionByZero",
        "'(1+2)÷0',                 DivisionByZero",
        // syntax
        "'(1+2',                    Syntax",
        "'1+2)',                    Syntax",
        "'1+',                      Syntax",
        "'×3',                      Syntax",
        "'',                        Syntax",
        // domain errors
        "'sqrt(-1)',                Domain",
        "'sqrt(-9)',                Domain",
        "'log(0)',                  Domain",
        "'log(-1)',                 Domain",
        "'ln(0)',                   Domain",
        "'ln(-1)',                  Domain",
        "'asin(2)',                 Domain",
        "'asin(-2)',                Domain",
        "'acos(2)',                 Domain",
        "'acos(-2)',                Domain",
        "'(-3)!',                   Domain",
        "'1001!',                   Domain",
    )
    fun errorPaths(expression: String, kind: String) {
        val result = rad.evaluate(expression)
        when (kind) {
            "DivisionByZero" ->
                assertInstanceOf(EvaluationResult.Error.DivisionByZero::class.java, result)

            "Syntax" ->
                assertInstanceOf(EvaluationResult.Error.Syntax::class.java, result)

            "Domain" ->
                assertInstanceOf(EvaluationResult.Error.Domain::class.java, result)

            "UnknownToken" ->
                assertInstanceOf(EvaluationResult.Error.UnknownToken::class.java, result)

            else -> error("unknown error kind: $kind")
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun EvaluationResult.expectSuccess(): BigDecimal {
        assertInstanceOf(
            EvaluationResult.Success::class.java,
            this,
            "expected Success but got $this",
        )
        return (this as EvaluationResult.Success).value
    }

    /**
     * BigDecimal compare with an absolute tolerance. 1e-10 catches the
     * Double-precision wobble in the last 5-6 digits without letting
     * genuinely wrong results through.
     */
    private fun assertNear(
        actual: BigDecimal,
        expected: String,
        tolerance: Double = 1e-10,
    ) {
        val diff = (actual - BigDecimal(expected)).abs().toDouble()
        assert(diff < tolerance) {
            "expected $expected (±$tolerance) but got $actual (diff $diff)"
        }
    }
}
