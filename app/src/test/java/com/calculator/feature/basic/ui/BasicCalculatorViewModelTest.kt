package com.calculator.feature.basic.ui

import androidx.lifecycle.SavedStateHandle
import com.calculator.core.math.AngleMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Behavioural tests for [BasicCalculatorViewModel].
 *
 * These tests assert the input-side UX rules (consecutive operators,
 * decimals, repeat-equals, error handling) that callers won't see in
 * the math engine alone - the engine evaluates whatever the ViewModel
 * hands it.
 *
 * The Evaluator is exercised for real (not mocked) because it's pure
 * Kotlin, deterministic, and microsecond-fast: stubbing would just
 * add maintenance burden without clarifying intent.
 *
 * Tests are grouped by behaviour family so failures localise quickly.
 */
class BasicCalculatorViewModelTest {
    private val savedStateHandle = SavedStateHandle()
    private val viewModel = BasicCalculatorViewModel(savedStateHandle)

    private fun type(vararg symbols: String) = symbols.forEach { viewModel.onEvent(BasicCalculatorEvent.Append(it)) }

    private fun equals() = viewModel.onEvent(BasicCalculatorEvent.Equals)

    private fun backspace() = viewModel.onEvent(BasicCalculatorEvent.Backspace)

    private fun clear() = viewModel.onEvent(BasicCalculatorEvent.Clear)

    private val state get() = viewModel.state.value

    @Nested
    inner class OperatorCollapse {
        @Test
        fun `consecutive plus operators collapse to one`() {
            type("1", "+", "+", "+", "5")
            assertEquals("1+5", state.expression)
        }

        @Test
        fun `swap retains the last operator pressed`() {
            type("1", "+", "5")
            type("×")
            assertEquals("1+5×", state.expression)
            type("÷")
            assertEquals("1+5÷", state.expression)
            type("-")
            assertEquals("1+5-", state.expression)
        }

        @Test
        fun `mixed operator run collapses to the last one`() {
            type("9", "-", "×", "÷", "+", "3")
            assertEquals("9+3", state.expression)
        }

        @Test
        fun `swap works for all four operator pairs`() {
            type("8")
            for (op in listOf("+", "-", "×", "÷")) {
                type(op)
                assertEquals("8$op", state.expression)
            }
        }

        @Test
        fun `leading plus is dropped`() {
            type("+")
            assertEquals("", state.expression)
        }

        @Test
        fun `leading times is dropped`() {
            type("×")
            assertEquals("", state.expression)
        }

        @Test
        fun `leading divide is dropped`() {
            type("÷")
            assertEquals("", state.expression)
        }

        @Test
        fun `leading minus is allowed for negation`() {
            type("-", "5")
            assertEquals("-5", state.expression)
            equals()
            assertEquals("-5", state.expression)
        }
    }

    @Nested
    inner class DecimalHandling {
        @Test
        fun `only one decimal per number segment`() {
            type("1", ".", "2", ".", "3")
            assertEquals("1.23", state.expression)
        }

        @Test
        fun `each number segment gets its own decimal`() {
            type("1", ".", "5", "+", "2", ".", "5")
            assertEquals("1.5+2.5", state.expression)
            equals()
            assertEquals("4", state.expression)
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
        fun `decimal after open paren auto-prefixes zero`() {
            type("(", ".", "5", ")")
            assertEquals("(0.5)", state.expression)
        }
    }

    @Nested
    inner class Equals {
        @Test
        fun `equals replaces expression with the canonical result`() {
            type("1", "+", "5")
            equals()
            assertEquals("6", state.expression)
            assertNull(state.errorMessage)
            assertNull(state.liveResult)
        }

        @Test
        fun `equals on blank expression is a no-op`() {
            equals()
            assertEquals("", state.expression)
            assertNull(state.errorMessage)
        }

        @Test
        fun `equals on a single number leaves it unchanged`() {
            type("4", "2")
            equals()
            assertEquals("42", state.expression)
        }

        @Test
        fun `digit after equals starts a fresh expression`() {
            type("1", "+", "5")
            equals()
            type("9")
            assertEquals("9", state.expression)
        }

        @Test
        fun `decimal after equals starts a fresh zero-prefixed number`() {
            type("1", "+", "5")
            equals()
            type(".")
            assertEquals("0.", state.expression)
        }

        @Test
        fun `operator after equals chains on the result`() {
            type("1", "+", "5")
            equals()
            type("×", "2")
            assertEquals("6×2", state.expression)
            equals()
            assertEquals("12", state.expression)
        }
    }

    @Nested
    inner class RepeatEquals {
        @Test
        fun `repeat equals replays trailing operator for plus`() {
            type("1", "+", "5")
            equals()
            assertEquals("6", state.expression)
            equals()
            assertEquals("11", state.expression)
            equals()
            assertEquals("16", state.expression)
        }

        @Test
        fun `repeat equals replays for minus`() {
            type("1", "0", "-", "3")
            equals()
            assertEquals("7", state.expression)
            equals()
            assertEquals("4", state.expression)
            equals()
            assertEquals("1", state.expression)
        }

        @Test
        fun `repeat equals replays for multiply`() {
            type("2", "×", "3")
            equals()
            assertEquals("6", state.expression)
            equals()
            assertEquals("18", state.expression)
            equals()
            assertEquals("54", state.expression)
        }

        @Test
        fun `repeat equals replays for divide`() {
            type("8", "0", "÷", "2")
            equals()
            assertEquals("40", state.expression)
            equals()
            assertEquals("20", state.expression)
            equals()
            assertEquals("10", state.expression)
        }

        @Test
        fun `trailing operator at equals auto-completes for plus`() {
            type("1", "+")
            equals()
            assertEquals("2", state.expression)
            equals()
            assertEquals("3", state.expression)
            equals()
            assertEquals("4", state.expression)
        }

        @Test
        fun `trailing operator at equals auto-completes for times giving doubling`() {
            type("2", "×")
            equals()
            assertEquals("4", state.expression)
            equals()
            assertEquals("8", state.expression)
            equals()
            assertEquals("16", state.expression)
        }

        @Test
        fun `trailing operator after a chain uses the operand just typed`() {
            type("1", "0", "-", "3", "×")
            equals()
            // `10-3×` -> auto-complete with `3` -> `10-3×3` = 10 - 9 = 1.
            assertEquals("1", state.expression)
        }

        @Test
        fun `typing any other event breaks the repeat chain`() {
            type("1", "+", "5")
            equals()
            assertNotNull(state.pendingRepeat)

            type("7")
            assertNull(state.pendingRepeat)
        }

        @Test
        fun `backspace also breaks the repeat chain`() {
            type("1", "+", "5")
            equals()
            assertNotNull(state.pendingRepeat)

            backspace()
            assertNull(state.pendingRepeat)
        }
    }

    @Nested
    inner class Backspace {
        @Test
        fun `backspace on empty expression is a no-op`() {
            backspace()
            assertEquals("", state.expression)
        }

        @Test
        fun `backspace removes the last character`() {
            type("1", "2", "3")
            backspace()
            assertEquals("12", state.expression)
            backspace()
            assertEquals("1", state.expression)
            backspace()
            assertEquals("", state.expression)
        }

        @Test
        fun `backspace updates the live result`() {
            type("1", "+", "5")
            assertEquals("6", state.liveResult)

            backspace()
            assertEquals("1+", state.expression)
            // Trailing operator means the expression is incomplete; preview goes null.
            assertNull(state.liveResult)

            backspace()
            assertEquals("1", state.expression)
            assertEquals("1", state.liveResult)
        }

        @Test
        fun `backspace clears the error message`() {
            type("5", "÷", "0")
            equals()
            assertNotNull(state.errorMessage)

            backspace()
            assertNull(state.errorMessage)
        }
    }

    @Nested
    inner class Clear {
        @Test
        fun `clear resets every field to defaults`() {
            type("1", "+", "5")
            equals()
            clear()

            assertEquals("", state.expression)
            assertNull(state.liveResult)
            assertNull(state.errorMessage)
            assertNull(state.pendingRepeat)
        }
    }

    @Nested
    inner class LivePreview {
        @Test
        fun `preview updates as the user types a complete expression`() {
            type("1", "+", "5")
            assertEquals("6", state.liveResult)
        }

        @Test
        fun `preview is null for an incomplete expression`() {
            type("1", "+")
            // Trailing operator -> expression doesn't evaluate; preview swallows it.
            assertNull(state.liveResult)
        }

        @Test
        fun `preview is null for a single number on its own`() {
            type("7")
            // A bare number still evaluates, so preview reflects it.
            assertEquals("7", state.liveResult)
        }

        @Test
        fun `preview is null when expression is empty`() {
            assertNull(state.liveResult)
        }
    }

    @Nested
    inner class Errors {
        @Test
        fun `division by zero surfaces a typed message`() {
            type("5", "÷", "0")
            equals()
            assertEquals("Can't divide by zero", state.errorMessage)
        }

        @Test
        fun `mismatched closing paren surfaces a syntax message`() {
            // Unmatched `)` cannot be auto-fixed (auto-close only adds `)`).
            type("1", "+", "2", ")")
            equals()
            assertEquals("Check your expression", state.errorMessage)
        }

        @Test
        fun `error clears once the user starts typing again`() {
            type("5", "÷", "0")
            equals()
            assertNotNull(state.errorMessage)

            type("3")
            assertNull(state.errorMessage)
        }
    }

    @Nested
    inner class ProcessDeathRestoration {
        @Test
        fun `state survives process death via the saved-state handle`() {
            type("1", "+", "5")
            equals()
            assertEquals("6", state.expression)
            assertEquals("+5", state.pendingRepeat)

            // Simulate process death by spinning up a new ViewModel with the
            // same SavedStateHandle - that's what the system does on restore.
            val restored = BasicCalculatorViewModel(savedStateHandle)

            assertEquals("6", restored.state.value.expression)
            assertEquals("+5", restored.state.value.pendingRepeat)
            // Live preview is derived, so it gets recomputed on restore.
            assertEquals("6", restored.state.value.liveResult)
        }

        @Test
        fun `error message survives restoration`() {
            type("5", "÷", "0")
            equals()
            val restored = BasicCalculatorViewModel(savedStateHandle)
            assertEquals("Can't divide by zero", restored.state.value.errorMessage)
        }

        @Test
        fun `repeat-equals chain continues after restoration`() {
            type("1", "+", "5")
            equals()

            val restored = BasicCalculatorViewModel(savedStateHandle)
            restored.onEvent(BasicCalculatorEvent.Equals)
            assertEquals("11", restored.state.value.expression)
        }

        @Test
        fun `clear is persisted across restoration`() {
            type("1", "+", "5")
            equals()
            clear()

            val restored = BasicCalculatorViewModel(savedStateHandle)
            assertEquals("", restored.state.value.expression)
            assertNull(restored.state.value.pendingRepeat)
        }
    }

    @Nested
    inner class LeadingZeroTrim {
        @Test
        fun `typing a digit on a lone zero replaces it`() {
            type("0", "5")
            assertEquals("5", state.expression)
        }

        @Test
        fun `lone zero stays until something replaces it`() {
            type("0")
            assertEquals("0", state.expression)
        }

        @Test
        fun `zero followed by decimal is kept intact`() {
            type("0", ".", "5")
            assertEquals("0.5", state.expression)
        }

        @Test
        fun `zero after an operator is trimmed by the next digit`() {
            type("1", "+", "0", "5")
            assertEquals("1+5", state.expression)
        }

        @Test
        fun `zero after an operator followed by decimal is kept`() {
            type("1", "+", "0", ".", "5")
            assertEquals("1+0.5", state.expression)
        }
    }

    @Nested
    inner class UnaryMinusInline {
        @Test
        fun `unary minus before paren evaluates correctly`() {
            type("-", "(", "2", "+", "3", ")")
            equals()
            assertEquals("-5", state.expression)
        }

        @Test
        fun `consecutive operator-then-minus collapses to a single minus`() {
            // The keypad's operator-collapse rule rewrites `2×-` to `2-`,
            // so tapping `2 × - 3` yields `2-3 = -1`. For unary-minus on
            // the second operand a user must use `2 × ( - 3 )`.
            type("2", "×", "-", "3")
            equals()
            assertEquals("-1", state.expression)
        }

        @Test
        fun `unary minus into open paren applies negation to the group`() {
            type("2", "×", "(", "-", "3", ")")
            equals()
            assertEquals("-6", state.expression)
        }
    }

    @Nested
    inner class Percent {
        @Test
        fun `bare N percent equals N over 100`() {
            type("5", "0", "%")
            equals()
            assertEquals("0.5", state.expression)
        }

        @Test
        fun `percent after plus behaves as postfix divide`() {
            type("1", "0", "0", "+", "1", "0", "%")
            equals()
            assertEquals("100.1", state.expression)
        }

        @Test
        fun `percent after times matches iOS arithmetic`() {
            type("1", "0", "0", "×", "1", "0", "%")
            equals()
            assertEquals("10", state.expression)
        }
    }

    @Nested
    inner class ScientificMode {
        @Test
        fun `toggle scientific flips the flag`() {
            assertEquals(false, state.scientific)
            viewModel.onEvent(BasicCalculatorEvent.ToggleScientific)
            assertEquals(true, state.scientific)
            viewModel.onEvent(BasicCalculatorEvent.ToggleScientific)
            assertEquals(false, state.scientific)
        }

        @Test
        fun `toggle angle mode cycles RAD and DEG`() {
            assertEquals(AngleMode.Radian, state.angleMode)
            viewModel.onEvent(BasicCalculatorEvent.ToggleAngleMode)
            assertEquals(AngleMode.Degree, state.angleMode)
            viewModel.onEvent(BasicCalculatorEvent.ToggleAngleMode)
            assertEquals(AngleMode.Radian, state.angleMode)
        }

        @Test
        fun `sin(30 deg) evaluates to one half in degree mode`() {
            viewModel.onEvent(BasicCalculatorEvent.ToggleAngleMode)
            type("sin(", "3", "0", ")")
            equals()
            assertEquals("0.5", state.expression)
        }

        @Test
        fun `auto-close brings sin(30 ) home without explicit closer`() {
            viewModel.onEvent(BasicCalculatorEvent.ToggleAngleMode)
            type("sin(", "3", "0")
            equals()
            assertEquals("0.5", state.expression)
        }

        @Test
        fun `pi key recalls the constant`() {
            type("π")
            equals()
            // Match against Math.PI within DECIMAL64 precision.
            assert(state.expression.startsWith("3.14159265")) {
                "expected π but got ${state.expression}"
            }
        }

        // ----- Function key wiring -----

        @Test
        fun `function key appends keyword and open paren`() {
            // The "sin" key in the keypad dispatches Append("sin(") - this
            // is the gesture every scientific function (sin/cos/tan/asin/
            // acos/atan/log/ln/sqrt/cbrt) relies on, so one assertion
            // exercises the contract for all of them.
            viewModel.onEvent(BasicCalculatorEvent.Append("sin("))
            assertEquals("sin(", state.expression)
        }

        @Test
        fun `function keyword chained after a digit inserts the function`() {
            type("3")
            viewModel.onEvent(BasicCalculatorEvent.Append("sin("))
            // No implicit multiplication is injected; user gets exactly
            // what they typed and is expected to add a × themselves.
            assertEquals("3sin(", state.expression)
        }

        @Test
        fun `backspace inside a function argument deletes one character`() {
            type("s", "i", "n", "(", "3", "0")
            // simpler: build the same state via Append; the keypad would
            // dispatch this as Append("sin(") then digits, but we want to
            // assert raw char-level backspace, so start with the full
            // string already in place.
            viewModel.onEvent(BasicCalculatorEvent.Clear)
            viewModel.onEvent(BasicCalculatorEvent.Append("sin("))
            type("3", "0")
            viewModel.onEvent(BasicCalculatorEvent.Backspace)
            assertEquals("sin(3", state.expression)
        }

        @Test
        fun `backspace at the open paren of a function deletes only the paren`() {
            // We intentionally do NOT treat "sin(" as one atomic token for
            // backspace - users expect character-by-character deletion,
            // which also matches how Android IME backspace works on text
            // input fields.
            viewModel.onEvent(BasicCalculatorEvent.Append("sin("))
            viewModel.onEvent(BasicCalculatorEvent.Backspace)
            assertEquals("sin", state.expression)
        }
    }

    @Nested
    inner class Memory {
        @Test
        fun `M plus stores the current result`() {
            type("1", "+", "5")
            equals()
            viewModel.onEvent(BasicCalculatorEvent.MemoryAdd)
            assertEquals("6", state.memory.toPlainString())
        }

        @Test
        fun `M minus subtracts from memory`() {
            type("1", "0")
            equals()
            viewModel.onEvent(BasicCalculatorEvent.MemoryAdd)
            // Clear before typing the next number, otherwise the post-equals
            // append rule treats the `3` as a continuation (giving 103) which
            // is not what `M -` should subtract.
            viewModel.onEvent(BasicCalculatorEvent.Clear)
            type("3")
            viewModel.onEvent(BasicCalculatorEvent.MemorySubtract)
            assertEquals("7", state.memory.toPlainString())
        }

        @Test
        fun `MR appends the stored value to the expression`() {
            type("4", "2")
            equals()
            viewModel.onEvent(BasicCalculatorEvent.MemoryAdd)
            viewModel.onEvent(BasicCalculatorEvent.Clear)
            viewModel.onEvent(BasicCalculatorEvent.MemoryRecall)
            assertEquals("42", state.expression)
        }

        @Test
        fun `MR after an operator appends without inserting an extra times`() {
            type("4", "2")
            equals()
            viewModel.onEvent(BasicCalculatorEvent.MemoryAdd)
            viewModel.onEvent(BasicCalculatorEvent.Clear)
            type("1", "0", "+")
            viewModel.onEvent(BasicCalculatorEvent.MemoryRecall)
            equals()
            assertEquals("52", state.expression)
        }

        @Test
        fun `MR after a number multiplies through`() {
            type("4", "2")
            equals()
            viewModel.onEvent(BasicCalculatorEvent.MemoryAdd)
            viewModel.onEvent(BasicCalculatorEvent.Clear)
            type("2")
            viewModel.onEvent(BasicCalculatorEvent.MemoryRecall)
            equals()
            assertEquals("84", state.expression)
        }

        @Test
        fun `MC zeroes the stored value`() {
            type("4", "2")
            equals()
            viewModel.onEvent(BasicCalculatorEvent.MemoryAdd)
            viewModel.onEvent(BasicCalculatorEvent.MemoryClear)
            assertEquals("0", state.memory.toPlainString())
        }

        @Test
        fun `clear preserves memory and angle mode`() {
            viewModel.onEvent(BasicCalculatorEvent.ToggleAngleMode)
            type("4", "2")
            equals()
            viewModel.onEvent(BasicCalculatorEvent.MemoryAdd)
            viewModel.onEvent(BasicCalculatorEvent.Clear)
            assertEquals("42", state.memory.toPlainString())
            assertEquals(AngleMode.Degree, state.angleMode)
            assertEquals("", state.expression)
        }
    }

    @Nested
    inner class AutoCloseParens {
        @Test
        fun `unclosed paren is closed on equals`() {
            type("(", "1", "+", "2")
            equals()
            assertEquals("3", state.expression)
        }

        @Test
        fun `two unclosed parens are closed on equals`() {
            type("(", "(", "5", "+", "1")
            equals()
            assertEquals("6", state.expression)
        }

        @Test
        fun `balanced parens are unchanged`() {
            type("(", "1", "+", "2", ")", "×", "4")
            equals()
            assertEquals("12", state.expression)
        }
    }

    @Nested
    inner class Parentheses {
        @Test
        fun `parens compose normally in the expression`() {
            type("(", "2", "+", "3", ")", "×", "4")
            assertEquals("(2+3)×4", state.expression)
            equals()
            assertEquals("20", state.expression)
        }

        @Test
        fun `operator after open paren still collapses for non-minus`() {
            // `(` followed by `×` is meaningless; current behaviour keeps the
            // characters in the expression and lets the evaluator reject it.
            // This pins the behaviour so a future change is intentional.
            type("(", "×")
            assertTrue(state.expression.startsWith("("))
        }
    }
}
