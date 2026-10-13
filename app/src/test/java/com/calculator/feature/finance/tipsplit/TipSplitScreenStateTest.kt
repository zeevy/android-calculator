package com.calculator.feature.finance.tipsplit

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Typed tip split values survive the app being killed and restored. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TipSplitScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typedValuesSurviveSaveAndRestore() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { TipSplitScreen(onOpenMenu = {}) }

        composeRule.onNodeWithText("1000").performTextReplacement("2500")
        composeRule.onNodeWithText("+").performClick()
        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("2500").assertExists()
        composeRule.onNodeWithText("3").assertExists()
    }
}
