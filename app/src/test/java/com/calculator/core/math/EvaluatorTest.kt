package com.calculator.core.math

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

/**
 * Unit tests for [Evaluator].
 *
 * These tests cover the contract callers actually rely on: precedence,
 * parentheses, decimals, unary minus and the typed error variants.
 *
 * Conventions:
 *  - Each test name reads as a sentence describing the property under test.
 *  - Parameterized cases use [CsvSource] so adding a new case is a one-line change.
 *  - Comparisons use [BigDecimal.compareTo] (`==` would fail on scale differences
 *    such as `1` vs `1.00`).
 */
class EvaluatorTest {
    private val evaluator = Evaluator()

    @DisplayName("arithmetic precedence and parentheses")
    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
        "'1+2',           3",
        "'2+3×4',         14",
        "'(2+3)×4',       20",
        "'10-4÷2',        8",
        "'10÷4',          2.5",
        "'-5+3',          -2",
        // `-(2+3)` and other unary-minus-before-`(` cases land in Phase 1 follow-up;
        // tokenizer currently only folds unary minus into a following digit.
        "'2×(3+(4-1))',   12",
        "'1.5+2.25',      3.75",
        "'100÷3',         33.33333333333333", // truncated to DECIMAL64 precision
    )
    fun evaluatesArithmeticCorrectly(
        expression: String,
        expected: String,
    ) {
        val result = evaluator.evaluate(expression)
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        val actual = (result as EvaluationResult.Success).value
        // Compare via BigDecimal.compareTo to ignore scale differences.
        assertEquals(0, actual.compareTo(BigDecimal(expected)), "expected $expected but got $actual")
    }

    @Test
    fun reportsDivisionByZero() {
        val result = evaluator.evaluate("5÷0")
        assertInstanceOf(EvaluationResult.Error.DivisionByZero::class.java, result)
    }

    @Test
    fun reportsMismatchedParenthesesAsSyntaxError() {
        val result = evaluator.evaluate("(1+2")
        assertInstanceOf(EvaluationResult.Error.Syntax::class.java, result)
    }

    @Test
    fun reportsDanglingOperatorAsSyntaxError() {
        val result = evaluator.evaluate("1+")
        assertInstanceOf(EvaluationResult.Error.Syntax::class.java, result)
    }

    @Test
    fun reportsEmptyExpressionAsSyntaxError() {
        val result = evaluator.evaluate("")
        assertInstanceOf(EvaluationResult.Error.Syntax::class.java, result)
    }

    @Test
    fun reportsUnknownCharacterAsUnknownToken() {
        val result = evaluator.evaluate("2@3")
        assertInstanceOf(EvaluationResult.Error.UnknownToken::class.java, result)
    }

    @Test
    fun whitespaceIsIgnored() {
        val result = evaluator.evaluate("  1  +  2  ")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal("3")))
    }

    @Test
    fun bothAsciiAndTypographicOperatorsAreAccepted() {
        // `*` `/` from a keyboard and `×` `÷` from the on-screen keypad must
        // produce identical results.
        val asciiResult = evaluator.evaluate("6*2/3")
        val typographicResult = evaluator.evaluate("6×2÷3")
        assertEquals(
            (asciiResult as EvaluationResult.Success).value.compareTo(
                (typographicResult as EvaluationResult.Success).value,
            ),
            0,
        )
    }

    @Test
    fun veryLargeProductsAreExactWithinDecimal64() {
        // 99999999 × 99999999 = 9999999800000001 - fits within DECIMAL64's
        // 16-digit precision and must be exact, no rounding.
        val result = evaluator.evaluate("99999999×99999999")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(
            0,
            (result as EvaluationResult.Success).value.compareTo(BigDecimal("9999999800000001")),
        )
    }

    @Test
    fun verySmallProductsPreserveSignificantDigits() {
        // 1e-7 × 1e-7 = 1e-14 - within DECIMAL64's representable range.
        val result = evaluator.evaluate("0.0000001×0.0000001")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(
            0,
            (result as EvaluationResult.Success).value.compareTo(BigDecimal("1E-14")),
        )
    }

    @Test
    fun chainedDivisionFollowsLeftToRightAssociativity() {
        // 24÷4÷2 should evaluate left-to-right (24÷4)÷2 = 3, not 24÷(4÷2) = 12.
        val result = evaluator.evaluate("24÷4÷2")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal("3")))
    }

    @Test
    fun negativeResultsAreRepresented() {
        val result = evaluator.evaluate("2-10")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal("-8")))
    }

    @Test
    fun zeroIsAcceptedAsAnOperand() {
        val result = evaluator.evaluate("0+0")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun zeroTimesAnythingIsZeroNotDivisionByZero() {
        // Make sure `0×5` doesn't accidentally hit the division-by-zero path.
        val result = evaluator.evaluate("0×5")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun multipleDecimalPointsInOneNumberAreRejected() {
        val result = evaluator.evaluate("1.2.3")
        assertInstanceOf(EvaluationResult.Error.UnknownToken::class.java, result)
    }

    // ----- Unary minus inline -----

    @Test
    fun unaryMinusBeforeParensNegatesTheGroup() {
        val result = evaluator.evaluate("-(2+3)")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal("-5")))
    }

    @Test
    fun unaryMinusAfterBinaryOperatorWorks() {
        val result = evaluator.evaluate("2*-3")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal("-6")))
    }

    @Test
    fun unaryMinusAfterAddOperatorWorks() {
        val result = evaluator.evaluate("5+-2")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal("3")))
    }

    @Test
    fun doubleUnaryMinusCancelsOut() {
        val result = evaluator.evaluate("--5")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal("5")))
    }

    @Test
    fun unaryMinusBeforeParensWithChain() {
        val result = evaluator.evaluate("10+-(2+3)")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal("5")))
    }

    // ----- Percentage -----

    @Test
    fun standalonePercentDividesByHundred() {
        val result = evaluator.evaluate("5%")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal("0.05")))
    }

    @Test
    fun percentAfterAddBehavesAsPostfixDivision() {
        // `100+10%` = `100 + 10÷100` = 100.1 (postfix-divide semantics,
        // not iOS-style `10% of 100`).
        val result = evaluator.evaluate("100+10%")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal("100.1")))
    }

    @Test
    fun percentAfterMultiplyMatchesIosBehaviour() {
        // `100×10%` = `100 × 10÷100` = 10. Same answer as iOS calculator
        // since `÷100` and `×0.10` are equivalent for the `× %` case.
        val result = evaluator.evaluate("100×10%")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal("10")))
    }

    @Test
    fun percentInsideParensDividesTheGroup() {
        val result = evaluator.evaluate("(2+3)%")
        assertInstanceOf(EvaluationResult.Success::class.java, result)
        assertEquals(0, (result as EvaluationResult.Success).value.compareTo(BigDecimal("0.05")))
    }
}
