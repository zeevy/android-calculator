package com.calculator.feature.basic.ui

import androidx.lifecycle.SavedStateHandle
import com.calculator.core.data.tape.TapeHolder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Covers the inline tape wiring: which key presses push a line, and
 * which reset gestures throw the tape away.
 *
 * [TapeHolder] is a process-wide singleton, so every test starts by
 * emptying it - otherwise entries leak between methods.
 */
class BasicCalculatorTapeTest {
    private lateinit var viewModel: BasicCalculatorViewModel

    @BeforeEach
    fun setUp() {
        TapeHolder.clear()
        viewModel = BasicCalculatorViewModel(SavedStateHandle())
    }

    @Test
    fun `successful equals appends a tape line`() {
        viewModel.onEvent(BasicCalculatorEvent.Append("2"))
        viewModel.onEvent(BasicCalculatorEvent.Append("+"))
        viewModel.onEvent(BasicCalculatorEvent.Append("5"))
        viewModel.onEvent(BasicCalculatorEvent.Equals)

        val line = viewModel.tape.value.single()
        assertEquals("2+5", line.expression)
        assertEquals("7", line.result)
    }

    @Test
    fun `tape keeps oldest first so the newest renders at the bottom`() {
        viewModel.onEvent(BasicCalculatorEvent.Append("1+1"))
        viewModel.onEvent(BasicCalculatorEvent.Equals)
        viewModel.onEvent(BasicCalculatorEvent.Append("2"))
        viewModel.onEvent(BasicCalculatorEvent.Append("×"))
        viewModel.onEvent(BasicCalculatorEvent.Append("3"))
        viewModel.onEvent(BasicCalculatorEvent.Equals)

        assertEquals(listOf("2", "6"), viewModel.tape.value.map { it.result })
    }

    @Test
    fun `error does not append a tape line`() {
        viewModel.onEvent(BasicCalculatorEvent.Append("5"))
        viewModel.onEvent(BasicCalculatorEvent.Append("÷"))
        viewModel.onEvent(BasicCalculatorEvent.Append("0"))
        viewModel.onEvent(BasicCalculatorEvent.Equals)

        assertTrue(viewModel.tape.value.isEmpty())
    }

    @Test
    fun `repeat-equals prints a tape line per press`() {
        // The display no longer echoes the committed expression, so a
        // `2 + = = =` chain would otherwise show 8 with nothing saying
        // where it came from. Unlike the durable history, the tape
        // records every replay.
        viewModel.onEvent(BasicCalculatorEvent.Append("2"))
        viewModel.onEvent(BasicCalculatorEvent.Append("+"))
        viewModel.onEvent(BasicCalculatorEvent.Equals)
        viewModel.onEvent(BasicCalculatorEvent.Equals)
        viewModel.onEvent(BasicCalculatorEvent.Equals)

        assertEquals(
            listOf("2+2" to "4", "4+2" to "6", "6+2" to "8"),
            viewModel.tape.value.map { it.expression to it.result },
        )
    }

    @Test
    fun `equals on a bare number prints nothing`() {
        // `7 =` computed nothing, so `7 = 7` is not a tape line no
        // matter how many times the key is pressed.
        viewModel.onEvent(BasicCalculatorEvent.Append("7"))
        viewModel.onEvent(BasicCalculatorEvent.Equals)
        viewModel.onEvent(BasicCalculatorEvent.Equals)
        viewModel.onEvent(BasicCalculatorEvent.Equals)

        assertTrue(viewModel.tape.value.isEmpty())
    }

    @Test
    fun `re-pressing equals after a real sum adds nothing further`() {
        // The reported bug: `5+6 =` prints one line, and the presses
        // after it used to keep printing `11 = 11`.
        viewModel.onEvent(BasicCalculatorEvent.Append("5+6"))
        viewModel.onEvent(BasicCalculatorEvent.Equals)
        viewModel.onEvent(BasicCalculatorEvent.Equals)
        viewModel.onEvent(BasicCalculatorEvent.Equals)
        viewModel.onEvent(BasicCalculatorEvent.Equals)

        assertEquals(listOf("5+6" to "11"), viewModel.tape.value.map { it.expression to it.result })
    }

    @Test
    fun `retyping the same sum records a second line`() {
        // Typing clears the just-committed marker, so a deliberate
        // repeat of the same calculation is a real entry - the tape is
        // a ledger, and two identical line items are two line items.
        viewModel.onEvent(BasicCalculatorEvent.Append("2+5"))
        viewModel.onEvent(BasicCalculatorEvent.Equals)
        viewModel.onEvent(BasicCalculatorEvent.Clear)
        viewModel.onEvent(BasicCalculatorEvent.Append("2+5"))
        viewModel.onEvent(BasicCalculatorEvent.Equals)

        assertEquals(2, viewModel.tape.value.size)
    }

    @Test
    fun `DeleteTapeLine drops only the targeted line`() {
        viewModel.onEvent(BasicCalculatorEvent.Append("1+1"))
        viewModel.onEvent(BasicCalculatorEvent.Equals)
        viewModel.onEvent(BasicCalculatorEvent.Append("2+5"))
        viewModel.onEvent(BasicCalculatorEvent.Equals)
        val doomed = viewModel.tape.value.first()

        viewModel.onEvent(BasicCalculatorEvent.DeleteTapeLine(doomed.id))

        assertEquals(listOf("7"), viewModel.tape.value.map { it.result })
    }

    @Test
    fun `ClearAll empties the tape`() {
        viewModel.onEvent(BasicCalculatorEvent.Append("2+5"))
        viewModel.onEvent(BasicCalculatorEvent.Equals)
        assertEquals(1, viewModel.tape.value.size)

        viewModel.onEvent(BasicCalculatorEvent.ClearAll)

        assertTrue(viewModel.tape.value.isEmpty())
        assertEquals("", viewModel.state.value.expression)
    }

    @Test
    fun `plain Clear resets the display but keeps the tape`() {
        viewModel.onEvent(BasicCalculatorEvent.Append("2+5"))
        viewModel.onEvent(BasicCalculatorEvent.Equals)

        // The app fires Clear programmatically when handing an expression
        // over from a tool page or a history row; those hand-offs must not
        // discard the session tape.
        viewModel.onEvent(BasicCalculatorEvent.Clear)

        assertEquals(1, viewModel.tape.value.size)
        assertEquals("", viewModel.state.value.expression)
    }
}
