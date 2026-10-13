package com.calculator.feature.finance.salary

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

/** The salary screen shows a result in each mode, field errors, and keeps the mode on restore. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-rUS")
class SalaryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultShowsTheNewSalary() {
        composeRule.setContent { CalculatorTheme { SalaryScreen(onOpenMenu = {}) } }

        composeRule.onNodeWithText("660,000.00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("55,000.00").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun hikeModeShowsTheHikePercent() {
        composeRule.setContent { CalculatorTheme { SalaryScreen(onOpenMenu = {}) } }

        composeRule.onNodeWithText("Hike %").performClick()

        composeRule.onNodeWithText("10 %").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun oldSalaryOfZeroShowsTheFieldError() {
        composeRule.setContent { CalculatorTheme { SalaryScreen(onOpenMenu = {}) } }
        composeRule.onNodeWithText("Hike %").performClick()

        composeRule.onNode(hasSetTextAction() and hasText("Old salary")).performTextReplacement("0")

        composeRule.onNodeWithText("Must be more than 0").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun hikeModeAndTypedValueSurviveRestore() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { CalculatorTheme { SalaryScreen(onOpenMenu = {}) } }
        composeRule.onNodeWithText("Hike %").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("New salary")).performTextReplacement("720000")

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("720000").assertExists()
        composeRule.onNodeWithText("20 %").performScrollTo().assertIsDisplayed()
    }
}
