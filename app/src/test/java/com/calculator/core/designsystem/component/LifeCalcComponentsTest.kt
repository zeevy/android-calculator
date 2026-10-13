package com.calculator.core.designsystem.component

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Accessibility of the shared life-calculator components. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LifeCalcComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun numberFieldCarriesItsLabelOnTheEditBox() {
        composeRule.setContent { LifeCalcNumberField(label = "Amount", value = "20", onValueChange = {}) }
        composeRule.onNode(hasSetTextAction() and hasContentDescription("Amount")).assertExists()
    }

    @Test
    fun segmentsAreRadioButtonsWithTheirState() {
        composeRule.setContent { LifeCalcSegmented(options = listOf("Metric", "Imperial"), selectedIndex = 1, onSelect = {}) }
        val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        composeRule.onNode(hasText("Imperial") and isRadio, useUnmergedTree = false).assertIsSelected()
    }
}
