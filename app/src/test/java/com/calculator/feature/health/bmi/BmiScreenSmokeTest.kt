package com.calculator.feature.health.bmi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.calculator.core.designsystem.theme.CalculatorTheme
import com.calculator.testing.FakeSettingsRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The BMI screen opens with a category and checks the inches range. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-rUS")
class BmiScreenSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        val viewModel = BmiViewModel(FakeSettingsRepository())
        composeRule.setContent { CalculatorTheme { BmiScreen(onOpenMenu = {}, viewModel = viewModel) } }
    }

    @Test
    fun defaultInputsShowACategory() {
        // 170 cm and 70 kg give a BMI of 24.22.
        composeRule.onNodeWithText("24.22").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Normal").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun inchesAboveElevenShowTheRangeError() {
        composeRule.onNodeWithText("ft·in").performClick()
        composeRule.onNode(hasSetTextAction() and hasContentDescription("Height (inches)")).performTextReplacement("15")

        composeRule.onNodeWithText("Must be from 0 to 11.99").performScrollTo().assertIsDisplayed()
    }
}
