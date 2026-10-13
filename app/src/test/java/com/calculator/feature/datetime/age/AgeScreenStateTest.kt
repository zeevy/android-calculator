package com.calculator.feature.datetime.age

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The picked date of birth survives the app being killed and restored. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-rUS")
class AgeScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pickedDateSurvivesSaveAndRestore() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { AgeScreen(onOpenMenu = {}) }
        composeRule.onNodeWithText("Monday, 15 January 1990").assertExists()

        composeRule.onNodeWithText("Tap to change").performClick()
        composeRule.onNodeWithText("January 20, 1990", substring = true).performClick()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.onNodeWithText("Saturday, 20 January 1990").assertExists()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Saturday, 20 January 1990").assertExists()
    }
}
