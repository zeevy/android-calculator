package com.calculator.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.util.Locale
import kotlin.test.assertEquals

/** The tools menu grid shows only the tools offered on the device. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ToolsGridTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        locale: Locale,
        onNavigate: (ToolRoute) -> Unit = {},
    ) = composeRule.setContent {
        ToolsGrid(
            tools = availableTools(locale, ZoneId.of("America/New_York")),
            current = null,
            scientific = false,
            onOpenCalculator = {},
            onOpenSheet = {},
            onNavigate = onNavigate,
        )
    }

    @Test
    fun gstTileIsAbsentForUsLocale() {
        show(Locale.US)
        composeRule.onNodeWithText("Loan").assertIsDisplayed()
        composeRule.onNodeWithText("GST").assertDoesNotExist()
    }

    @Test
    fun gstTileOpensGstForIndianLocale() {
        var opened: ToolRoute? = null
        show(Locale.forLanguageTag("en-IN")) { opened = it }
        composeRule.onNodeWithText("GST").performClick()
        assertEquals(GstRoute, opened)
    }
}
