package com.calculator.feature.finance.deposit

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

/** The deposits screen shows each deposit's maturity, field errors, and keeps typed values. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-rUS")
class DepositScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show() = composeRule.setContent { CalculatorTheme { DepositScreen(onOpenMenu = {}) } }

    @Test
    fun defaultFdShowsTheMaturity() {
        show()

        composeRule.onNodeWithText("Maturity amount").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("141,477.82").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("41,477.82").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun rdShowsTheMaturityAndTotalDeposited() {
        show()
        composeRule.onNodeWithText("RD").performClick()

        composeRule.onNodeWithText("12,462.13").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("12,000.00").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun ppfShowsTheMaturityAndTheYearTable() {
        show()
        composeRule.onNodeWithText("PPF").performClick()

        composeRule.onNodeWithText("4,068,209.22").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("YEAR BY YEAR").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("4,068,209").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun ppfDepositBelowTheLimitShowsTheRange() {
        show()
        composeRule.onNodeWithText("PPF").performClick()

        composeRule.onNode(hasSetTextAction() and hasText("Yearly deposit")).performTextReplacement("400")

        composeRule.onNodeWithText("Must be from 500 to 150,000").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("YEAR BY YEAR").assertDoesNotExist()
    }

    @Test
    fun eachModeKeepsItsTypedValuesAcrossSwitchesAndRestore() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { CalculatorTheme { DepositScreen(onOpenMenu = {}) } }
        composeRule.onNode(hasSetTextAction() and hasText("Deposit amount")).performTextReplacement("200000")
        composeRule.onNodeWithText("RD").performClick()
        composeRule.onNodeWithText("FD").performClick()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("200000").assertExists()
        composeRule.onNodeWithText("282,955.64").performScrollTo().assertIsDisplayed()
    }
}
