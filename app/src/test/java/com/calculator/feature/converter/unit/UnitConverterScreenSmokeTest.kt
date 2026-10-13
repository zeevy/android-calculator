package com.calculator.feature.converter.unit

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import com.calculator.core.data.converter.UnitConverterRepository
import com.calculator.core.designsystem.theme.CalculatorTheme
import com.calculator.core.domain.converter.UnitCategory
import com.calculator.testing.FakeSettingsRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Typing in the unit converter's from field fills the to field. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-rUS")
class UnitConverterScreenSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun thousandMetresShowAsOneKilometre() {
        val viewModel = UnitConverterViewModel(NoRecentPairs(), FakeSettingsRepository(), SavedStateHandle())
        composeRule.setContent { CalculatorTheme { UnitConverterScreen(onOpenMenu = {}, viewModel = viewModel) } }
        composeRule.waitForIdle()

        // The first field is From (metres), the second is To (kilometres).
        val fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement("1000")

        fields[1].assertTextEquals("1")
    }

    /** No saved unit pairs, so Length opens on metres to kilometres. */
    private class NoRecentPairs : UnitConverterRepository {
        override suspend fun recent(category: UnitCategory): Pair<String, String>? = null

        override suspend fun record(category: UnitCategory, fromSymbol: String, toSymbol: String) = Unit
    }
}
