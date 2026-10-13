package com.calculator.feature.finance.discount

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Typed discount values and the source field survive the app being killed and restored. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DiscountScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typedFinalPriceStaysTheSourceAfterRestore() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { DiscountScreen(onOpenMenu = {}) }

        composeRule.onNodeWithText("1600").performTextReplacement("1500")
        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("1500").assertExists()
        composeRule.onNodeWithText("25").assertExists()
    }
}
