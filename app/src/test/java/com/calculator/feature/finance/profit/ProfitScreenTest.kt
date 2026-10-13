package com.calculator.feature.finance.profit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.calculator.core.designsystem.theme.CalculatorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The profit screen shows profit or loss, the margin field error, and keeps the mode on restore. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-rUS")
class ProfitScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultPricesShowTheProfitMarkupAndMargin() {
        composeRule.setContent { CalculatorTheme { ProfitScreen(onOpenMenu = {}) } }

        composeRule.onNodeWithText("Profit").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("200.00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("25 %").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("20 %").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sellingBelowCostShowsTheLoss() {
        composeRule.setContent { CalculatorTheme { ProfitScreen(onOpenMenu = {}) } }

        composeRule.onNode(hasSetTextAction() and hasText("Selling price")).performTextReplacement("600")

        composeRule.onNodeWithText("Loss").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Loss %").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("200.00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Profit").assertDoesNotExist()
    }

    @Test
    fun marginOf100ShowsTheFieldError() {
        composeRule.setContent { CalculatorTheme { ProfitScreen(onOpenMenu = {}) } }
        composeRule.onNodeWithText("Margin").performClick()

        composeRule.onNode(hasSetTextAction() and hasText("Margin on selling price")).performTextReplacement("100")

        composeRule.onNodeWithText("Must be below 100").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun markupModeAndTypedValueSurviveRestore() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { CalculatorTheme { ProfitScreen(onOpenMenu = {}) } }
        composeRule.onNodeWithText("Markup").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("Markup on cost")).performTextReplacement("50")

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("50").assertExists()
        // 800 x 1.5
        composeRule.onNodeWithText("1,200.00").performScrollTo().assertIsDisplayed()
    }
}
