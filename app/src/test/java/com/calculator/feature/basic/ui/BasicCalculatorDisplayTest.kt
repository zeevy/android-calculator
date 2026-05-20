package com.calculator.feature.basic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Headless Compose UI test for the calculator's [Display] composable.
 *
 * Runs under Robolectric in the regular JVM `test` source set, so the
 * full suite kicks off via `./gradlew test` with no emulator, device,
 * or instrumented setup. The trade-off vs an instrumented test is a
 * ~3s Robolectric boot per test class, after which individual test
 * methods run in tens of milliseconds.
 *
 * Each test renders the [Display] composable directly with a hand-
 * built [BasicCalculatorUiState] and asserts the user-visible text -
 * exactly what would otherwise be checked manually by typing into the
 * device and reading the screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BasicCalculatorDisplayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_showsZeroOnBottomRow() {
        composeRule.setContent {
            Display(
                expression = "",
                preview = null,
                error = null,
                lastCommittedExpression = null,
                lastValidPreview = null,
            )
        }
        composeRule.onNodeWithText("0").assertIsDisplayed()
    }

    @Test
    fun midExpression_showsExpressionOnTop_andPreviewOnBottom() {
        composeRule.setContent {
            Display(
                expression = "5+3",
                preview = "8",
                error = null,
                lastCommittedExpression = null,
                lastValidPreview = "8",
            )
        }
        composeRule.onNodeWithText("5+3").assertIsDisplayed()
        composeRule.onNodeWithText("8").assertIsDisplayed()
    }

    @Test
    fun afterEquals_topShowsCommittedExpression_bottomShowsResult() {
        // Mirrors the post-`=` state machine: lastCommittedExpression
        // wins on top, lastValidPreview surfaces on the bottom even
        // though the live preview is null.
        composeRule.setContent {
            Display(
                expression = "8",
                preview = null,
                error = null,
                lastCommittedExpression = "5+3",
                lastValidPreview = "8",
            )
        }
        composeRule.onNodeWithText("5+3").assertIsDisplayed()
        composeRule.onNodeWithText("8").assertIsDisplayed()
    }

    @Test
    fun expressionEndsInOperator_holdsLastValidPreview_neverBlanks() {
        // The regression we're guarding against: typing `5+3+` should
        // not blank the bottom row. lastValidPreview holds the `8`
        // from the previous evaluable state.
        composeRule.setContent {
            Display(
                expression = "5+3+",
                preview = null,
                error = null,
                lastCommittedExpression = null,
                lastValidPreview = "8",
            )
        }
        composeRule.onNodeWithText("5+3+").assertIsDisplayed()
        composeRule.onNodeWithText("8").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessageOnBottom() {
        composeRule.setContent {
            Display(
                expression = "5÷0",
                preview = null,
                error = "Can't divide by zero",
                lastCommittedExpression = null,
                lastValidPreview = null,
            )
        }
        composeRule.onNodeWithText("5÷0").assertIsDisplayed()
        composeRule.onNodeWithText("Can't divide by zero").assertIsDisplayed()
    }
}
