package com.calculator.feature.basic.ui

import com.calculator.core.math.Evaluator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Behavioural tests for [BasicCalculatorViewModel].
 *
 * These tests assert the input-side UX rules (consecutive operators,
 * decimals, repeat-equals) that callers won't see in the math engine
 * alone - the engine evaluates whatever the ViewModel hands it.
 *
 * The Evaluator is exercised for real (not mocked) because it's pure
 * Kotlin, deterministic, and microsecond-fast: stubbing would just
 * add maintenance burden without clarifying intent.
 */
class BasicCalculatorViewModelTest {
    private val viewModel = BasicCalculatorViewModel(Evaluator())

    private fun type(vararg symbols: String) = symbols.forEach { viewModel.onEvent(BasicCalculatorEvent.Append(it)) }

    private fun equals() = viewModel.onEvent(BasicCalculatorEvent.Equals)

    private fun clear() = viewModel.onEvent(BasicCalculatorEvent.Clear)

    private val state get() = viewModel.state.value

    @Test
    fun `consecutive plus operators collapse to one`() {
        type("1", "+", "+", "+", "5")
        assertEquals("1+5", state.expression)
    }

    @Test
    fun `swapping operator replaces the previous one`() {
        type("1", "+", "5")
        type("×")
        assertEquals("1+5×", state.expression)

        type("÷")
        assertEquals("1+5÷", state.expression)
    }

    @Test
    fun `consecutive operators collapse for minus and times and divide`() {
        type("9", "-", "×", "÷", "3")
        assertEquals("9÷3", state.expression)
    }

    @Test
    fun `leading plus times and divide are dropped but leading minus is allowed`() {
        type("+", "×", "÷")
        assertEquals("", state.expression)

        type("-", "5")
        assertEquals("-5", state.expression)
    }

    @Test
    fun `only one decimal point per number segment`() {
        type("1", ".", "2", ".", "3")
        assertEquals("1.23", state.expression)
    }

    @Test
    fun `leading decimal auto-prefixes zero`() {
        type(".", "5")
        assertEquals("0.5", state.expression)
    }

    @Test
    fun `decimal after operator auto-prefixes zero`() {
        type("1", "+", ".", "5")
        assertEquals("1+0.5", state.expression)
    }

    @Test
    fun `equals replaces expression with result`() {
        type("1", "+", "5")
        equals()
        assertEquals("6", state.expression)
        assertNull(state.errorMessage)
    }

    @Test
    fun `digit after equals starts a fresh expression`() {
        type("1", "+", "5")
        equals()
        type("9")
        assertEquals("9", state.expression)
    }

    @Test
    fun `operator after equals chains on the result`() {
        type("1", "+", "5")
        equals()
        type("×", "2")
        assertEquals("6×2", state.expression)
    }

    @Test
    fun `repeat equals replays the trailing operator for plus`() {
        type("1", "+", "5")
        equals()
        assertEquals("6", state.expression)
        equals()
        assertEquals("11", state.expression)
        equals()
        assertEquals("16", state.expression)
    }

    @Test
    fun `repeat equals replays for multiply giving doubling for two`() {
        type("2", "×")
        equals()
        assertEquals("4", state.expression)
        equals()
        assertEquals("8", state.expression)
        equals()
        assertEquals("16", state.expression)
    }

    @Test
    fun `trailing operator at equals auto-completes with first operand`() {
        type("1", "+")
        equals()
        assertEquals("2", state.expression)
        equals()
        assertEquals("3", state.expression)
    }

    @Test
    fun `trailing operator after a chain auto-completes from the operand before it`() {
        type("1", "0", "-", "3", "×")
        equals()
        // `10-3×` auto-completes to `10-3×3` (the operand just entered before
        // the dangling `×` becomes the implicit second operand).
        // Result: 10 - (3×3) = 10 - 9 = 1.
        assertEquals("1", state.expression)
    }

    @Test
    fun `clear resets state to default`() {
        type("1", "+", "5")
        equals()
        clear()
        assertEquals("", state.expression)
        assertNull(state.liveResult)
        assertNull(state.errorMessage)
        assertNull(state.pendingRepeat)
    }

    @Test
    fun `division by zero surfaces a typed error message`() {
        type("5", "÷", "0")
        equals()
        assertEquals("Can't divide by zero", state.errorMessage)
    }
}
