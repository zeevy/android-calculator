package com.calculator.feature.finance.gst

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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

/** The GST screen opens with a result and shows a field error for bad input. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-rUS")
class GstScreenSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        val viewModel = GstViewModel(FakeSettingsRepository())
        composeRule.setContent { CalculatorTheme { GstScreen(onOpenMenu = {}, viewModel = viewModel) } }
    }

    @Test
    fun defaultInputsShowTheTaxSplit() {
        composeRule.onNodeWithText("CGST").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("SGST").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Gross").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun invalidNetAmountShowsTheFieldError() {
        composeRule.onNode(hasSetTextAction() and hasContentDescription("Net amount")).performTextReplacement("12abc")

        composeRule.onNodeWithText("Not a valid number").performScrollTo().assertIsDisplayed()
    }
}
