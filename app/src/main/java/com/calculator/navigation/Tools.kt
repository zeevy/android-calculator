package com.calculator.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.ui.graphics.vector.ImageVector
import com.calculator.R
import java.time.ZoneId
import java.util.Locale

/**
 * One tool page, as the tools menu and the launcher shortcuts see it.
 *
 * @property id Stable id used in launcher shortcuts and the saved recents
 *   list. Never rename one: saved shortcuts would stop working.
 * @property route Destination opened by the menu tile and the shortcut.
 * @property label Tile label and short shortcut label.
 * @property longLabel Long shortcut label.
 * @property icon Tile icon.
 * @property isAvailable Whether the tool is offered for this locale and
 *   time zone. India-only tools use [isIndia].
 */
internal data class Tool(
    val id: String,
    val route: ToolRoute,
    @param:StringRes val label: Int,
    @param:StringRes val longLabel: Int = label,
    val icon: ImageVector,
    val isAvailable: (Locale, ZoneId) -> Boolean = { _, _ -> true },
)

/**
 * True for a device set up for India: the locale's country is IN, or the
 * time zone is India's. Many Indian phones run English (United States),
 * so the locale alone would miss them.
 */
internal fun isIndia(
    locale: Locale,
    zone: ZoneId,
): Boolean = locale.country == "IN" || zone.id in INDIA_ZONE_IDS

private val INDIA_ZONE_IDS = setOf("Asia/Kolkata", "Asia/Calcutta")

/**
 * Every tool page, in menu order. Home, Advanced, History and Settings
 * are not here: they are not pages of their own.
 *
 * To add a tool: add a route in Destinations.kt, one entry here, one
 * `composable<...>` line in [CalculatorNavHost], and its strings.
 */
internal val Tools: List<Tool> =
    listOf(
        Tool("units", UnitConverterRoute, R.string.tool_units, R.string.shortcut_units_long, Icons.Filled.Straighten),
        Tool("loan", LoanRoute, R.string.tool_loan, icon = Icons.Filled.AccountBalance),
        Tool("gst", GstRoute, R.string.tool_gst, icon = Icons.Filled.Receipt, isAvailable = ::isIndia),
        Tool("discount", DiscountRoute, R.string.tool_discount, icon = Icons.Filled.LocalOffer),
        Tool("tipsplit", TipSplitRoute, R.string.tool_tipsplit, icon = Icons.Filled.Restaurant),
        Tool("investment", InvestmentRoute, R.string.tool_investment, icon = Icons.Filled.Savings),
        Tool("percent", PercentRoute, R.string.tool_percent, icon = Icons.Filled.Percent),
        Tool("bases", BaseConverterRoute, R.string.tool_basecalc, icon = Icons.Filled.Code),
        Tool("bmi", BmiRoute, R.string.tool_bmi, icon = Icons.Filled.MonitorWeight),
        Tool("age", AgeRoute, R.string.tool_age, icon = Icons.Filled.Cake),
        Tool("datediff", DateDiffRoute, R.string.tool_date_diff, icon = Icons.Filled.DateRange),
        Tool("timezone", TimezoneRoute, R.string.tool_timezone, icon = Icons.Filled.Public),
        Tool("ovulation", OvulationRoute, R.string.tool_ovulation, icon = Icons.Filled.Favorite),
        Tool("tape", TapeRoute, R.string.tool_tape, icon = Icons.AutoMirrored.Filled.Notes),
    )

/** Tools offered for [locale] and [zone], in menu order. */
internal fun availableTools(
    locale: Locale,
    zone: ZoneId,
): List<Tool> = Tools.filter { it.isAvailable(locale, zone) }

/** The tool with this shortcut [id], if it is offered for [locale] and [zone]. */
internal fun availableToolById(
    id: String,
    locale: Locale,
    zone: ZoneId,
): Tool? = Tools.firstOrNull { it.id == id }?.takeIf { it.isAvailable(locale, zone) }
