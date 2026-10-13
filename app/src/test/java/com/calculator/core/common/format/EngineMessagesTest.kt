package com.calculator.core.common.format

import com.calculator.R
import com.calculator.core.math.EvaluationResult
import com.calculator.core.math.Evaluator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Engine errors map to the message for the rule the user broke. */
class EngineMessagesTest {
    private fun messageFor(expression: String) = (Evaluator().evaluate(expression) as EvaluationResult.Error).messageRes()

    @Test
    fun `each error kind has its own message`() {
        assertEquals(R.string.basic_error_division_by_zero, messageFor("5÷0"))
        assertEquals(R.string.basic_error_division_by_zero, messageFor("0÷0"))
        assertEquals(R.string.basic_error_sqrt, messageFor("sqrt(-4)"))
        assertEquals(R.string.basic_error_log, messageFor("log(0)"))
        assertEquals(R.string.basic_error_factorial_negative, messageFor("(-3)!"))
        assertEquals(R.string.basic_error_factorial_whole, messageFor("2.5!"))
        assertEquals(R.string.basic_error_syntax, messageFor("5+×"))
    }
}
