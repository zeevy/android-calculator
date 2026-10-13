package com.calculator.feature.finance.loan

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Typed loan values survive the app being killed and restored. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LoanScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typedValuesSurviveSaveAndRestore() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { LoanScreen(onOpenMenu = {}) }

        composeRule.onNodeWithText("100000").performTextReplacement("250000")
        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("250000").assertExists()
    }
}
