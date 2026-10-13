package com.calculator.feature.finance.investment

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

/** The investment screen shows a result in each mode, field errors, and keeps the mode on restore. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-rUS")
class InvestmentScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultSipShowsTheMaturity() {
        composeRule.setContent { CalculatorTheme { InvestmentScreen(onOpenMenu = {}) } }

        composeRule.onNodeWithText("Maturity value").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1,161,695.38").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun cagrModeShowsTheGrowthRate() {
        composeRule.setContent { CalculatorTheme { InvestmentScreen(onOpenMenu = {}) } }

        composeRule.onNodeWithText("CAGR").performClick()

        composeRule.onNodeWithText("CAGR (per year)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("14.87 %").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("100 %").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun cagrStartValueOfZeroShowsTheFieldError() {
        composeRule.setContent { CalculatorTheme { InvestmentScreen(onOpenMenu = {}) } }
        composeRule.onNodeWithText("CAGR").performClick()

        composeRule.onNode(hasSetTextAction() and hasText("Start value")).performTextReplacement("0")

        composeRule.onNodeWithText("Must be more than 0").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun cagrModeAndTypedValueSurviveRestore() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { CalculatorTheme { InvestmentScreen(onOpenMenu = {}) } }
        composeRule.onNodeWithText("CAGR").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("End value")).performTextReplacement("400000")

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("400000").assertExists()
        // 4^(1/5) - 1
        composeRule.onNodeWithText("31.95 %").performScrollTo().assertIsDisplayed()
    }
}
