package com.calculator.feature.basic.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.calculator.core.data.tape.TapeEntry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Headless Compose UI test for the inline tape rendered above the
 * calculator display. Runs under Robolectric in the JVM `test` source
 * set, same as the other basic-calculator UI tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InlineTapeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersOneLinePerEntry_asExpressionEqualsResult() {
        composeRule.setContent {
            InlineTape(
                entries =
                    listOf(
                        TapeEntry(id = 0, expression = "1+1", result = "2"),
                        TapeEntry(id = 1, expression = "2+5", result = "7"),
                    ),
                onRecall = {},
                onDelete = {},
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
        }

        composeRule.onNodeWithText("1+1 = 2").assertIsDisplayed()
        composeRule.onNodeWithText("2+5 = 7").assertIsDisplayed()
    }

    @Test
    fun tappingALine_recallsItsResult() {
        var recalled: String? = null
        composeRule.setContent {
            InlineTape(
                entries = listOf(TapeEntry(id = 0, expression = "2+5", result = "7")),
                onRecall = { recalled = it },
                onDelete = {},
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
        }

        composeRule.onNodeWithText("2+5 = 7").performClick()

        assert(recalled == "7") { "expected the tapped line's result, got $recalled" }
    }

    @Test
    fun emptyTape_rendersNothing() {
        composeRule.setContent {
            InlineTape(
                entries = emptyList(),
                onRecall = {},
                onDelete = {},
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
        }

        composeRule.onNodeWithText("=").assertDoesNotExist()
    }
}
