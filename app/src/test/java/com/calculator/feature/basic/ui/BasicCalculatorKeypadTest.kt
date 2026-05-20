package com.calculator.feature.basic.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.calculator.core.designsystem.theme.CalculatorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Headless Compose UI test for the calculator's [Keypad] composable.
 *
 * Runs on Robolectric, no device or emulator. Each test mounts the
 * keypad with a fake event sink and asserts that clicking a button
 * fires the right [BasicCalculatorEvent]. This catches keypad/event
 * regressions (wrong wiring, missing keys, scientific-mode toggle
 * bugs) without going near the math engine or the ViewModel.
 *
 * Convention: tests collect events into a [MutableList] and use plain
 * JUnit assertions. The Compose interaction APIs run on the test main
 * thread - no need for `runTest` or coroutine plumbing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BasicCalculatorKeypadTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun digitKey_emitsAppendEvent() {
        val events = mutableListOf<BasicCalculatorEvent>()
        composeRule.setContent {
            CalculatorTheme { Keypad(scientific = false, onEvent = events::add) }
        }
        composeRule.onNodeWithText("7").performClick()
        assertEquals(listOf<BasicCalculatorEvent>(BasicCalculatorEvent.Append("7")), events)
    }

    @Test
    fun equalsKey_emitsEqualsEvent() {
        val events = mutableListOf<BasicCalculatorEvent>()
        composeRule.setContent {
            CalculatorTheme { Keypad(scientific = false, onEvent = events::add) }
        }
        composeRule.onNodeWithText("=").performClick()
        assertEquals(listOf<BasicCalculatorEvent>(BasicCalculatorEvent.Equals), events)
    }

    @Test
    fun operatorKeys_emitAppendEvents() {
        val events = mutableListOf<BasicCalculatorEvent>()
        composeRule.setContent {
            CalculatorTheme { Keypad(scientific = false, onEvent = events::add) }
        }
        composeRule.onNodeWithText("+").performClick()
        composeRule.onNodeWithText("-").performClick()
        composeRule.onNodeWithText("×").performClick()
        composeRule.onNodeWithText("÷").performClick()
        assertEquals(
            listOf<BasicCalculatorEvent>(
                BasicCalculatorEvent.Append("+"),
                BasicCalculatorEvent.Append("-"),
                BasicCalculatorEvent.Append("×"),
                BasicCalculatorEvent.Append("÷"),
            ),
            events,
        )
    }

    @Test
    fun signFlipKey_emitsSignFlipEvent() {
        val events = mutableListOf<BasicCalculatorEvent>()
        composeRule.setContent {
            CalculatorTheme { Keypad(scientific = false, onEvent = events::add) }
        }
        composeRule.onNodeWithText("±").performClick()
        assertEquals(listOf<BasicCalculatorEvent>(BasicCalculatorEvent.SignFlip), events)
    }

    @Test
    fun factorialKey_isNotPresentInBasicMode() {
        val events = mutableListOf<BasicCalculatorEvent>()
        composeRule.setContent {
            CalculatorTheme { Keypad(scientific = false, onEvent = events::add) }
        }
        // x! was removed from the basic-mode keypad alongside the tall
        // + redesign - factorial is still available in scientific mode
        // via the Factorial key in the scientific surface, but the
        // basic keypad no longer carries a dedicated tile.
        assertEquals(0, composeRule.onAllNodesWithText("x!").fetchSemanticsNodes().size)
    }

    @Test
    fun scientificMode_exposesAdditionalFunctionKeys() {
        val events = mutableListOf<BasicCalculatorEvent>()
        composeRule.setContent {
            CalculatorTheme { Keypad(scientific = true, onEvent = events::add) }
        }
        // These four are scientific-only and should not exist in basic mode.
        composeRule.onNodeWithText("sin").performClick()
        composeRule.onNodeWithText("cos").performClick()
        composeRule.onNodeWithText("√").performClick()
        composeRule.onNodeWithText("π").performClick()

        assertEquals(4, events.size)
        // Functions append `name(` so the cursor sits inside an
        // unbalanced parenthesis ready for the operand.
        assertEquals(BasicCalculatorEvent.Append("sin("), events[0])
        assertEquals(BasicCalculatorEvent.Append("cos("), events[1])
        assertEquals(BasicCalculatorEvent.Append("sqrt("), events[2])
        assertEquals(BasicCalculatorEvent.Append("π"), events[3])
    }
}
