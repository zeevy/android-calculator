package com.calculator.core.math

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Keypad input rules shared by the app, the widget and the floating calculator. */
class ExpressionEditorTest {
    private fun type(vararg keys: String): String = keys.fold("") { expr, key -> ExpressionEditor.append(expr, key) }

    @Test
    fun `consecutive operators collapse to the last one`() {
        assertEquals("1×", type("1", "+", "+", "×"))
    }

    @Test
    fun `a leading operator other than minus is ignored`() {
        assertEquals("5", type("+", "×", "5"))
        assertEquals("-5", type("-", "5"))
    }

    @Test
    fun `one decimal point per number, with a zero prefix`() {
        assertEquals("1.5", type("1", ".", "5", "."))
        assertEquals("0.5", type(".", "5"))
        assertEquals("2+0.1", type("2", "+", ".", "1"))
    }

    @Test
    fun `a lone leading zero is replaced`() {
        assertEquals("7", type("0", "7"))
        assertEquals("3+7", type("3", "+", "0", "7"))
        assertEquals("10", type("1", "0"))
    }

    @Test
    fun `after equals a digit starts over and an operator chains`() {
        assertEquals("9", ExpressionEditor.append("14", "9", afterEquals = true))
        assertEquals("14+", ExpressionEditor.append("14", "+", afterEquals = true))
        assertEquals("0.", ExpressionEditor.append("14", ".", afterEquals = true))
        assertEquals("100%", ExpressionEditor.append("100", "%", afterEquals = true))
        assertEquals("5!", ExpressionEditor.append("5", "!", afterEquals = true))
        assertEquals("3^2", ExpressionEditor.append("3", "^2", afterEquals = true))
    }

    @Test
    fun `flipSign toggles the trailing number`() {
        assertEquals("-", ExpressionEditor.flipSign(""))
        assertEquals("5+-3", ExpressionEditor.flipSign("5+3"))
        assertEquals("5+3", ExpressionEditor.flipSign("5+-3"))
        assertEquals("10+-", ExpressionEditor.flipSign("10+"))
        assertEquals("(2)", ExpressionEditor.flipSign("(2)"))
    }

    @Test
    fun `equals closes parentheses and completes a trailing operator`() {
        assertEquals("(1+2)" to "", ExpressionEditor.completeForEquals("(1+2"))
        assertEquals("1+1" to "+1", ExpressionEditor.completeForEquals("1+"))
        assertEquals("-" to null, ExpressionEditor.completeForEquals("-"))
    }

    @Test
    fun `operator precedence holds through the shared evaluator`() {
        val (equation, _) = ExpressionEditor.completeForEquals(type("2", "+", "3", "×", "4"))
        val result = Evaluator().evaluate(equation) as EvaluationResult.Success
        assertEquals("14", ExpressionEditor.resultText(result.value))
    }
}
