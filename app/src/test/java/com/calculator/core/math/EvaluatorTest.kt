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
}
