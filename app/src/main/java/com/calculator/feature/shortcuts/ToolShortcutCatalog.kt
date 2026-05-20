package com.calculator.feature.shortcuts

import com.calculator.R
import com.calculator.navigation.AgeRoute
import com.calculator.navigation.BaseConverterRoute
import com.calculator.navigation.BmiRoute
import com.calculator.navigation.DateDiffRoute
import com.calculator.navigation.DiscountRoute
import com.calculator.navigation.GstRoute
import com.calculator.navigation.InvestmentRoute
import com.calculator.navigation.LoanRoute
import com.calculator.navigation.OvulationRoute
import com.calculator.navigation.PercentRoute
import com.calculator.navigation.TapeRoute
import com.calculator.navigation.TimezoneRoute
import com.calculator.navigation.TipSplitRoute
import com.calculator.navigation.UnitConverterRoute

/**
 * Stable ID + display labels for every tool that can show up as a
 * launcher shortcut.
 *
 * Two-way mapping:
 *  - [byId] looks up an entry from a shortcut intent extra so
 *    MainActivity / CalculatorNavHost can route the deep-link.
 *  - [idOf] turns a navigation [Route] into the stable ID used as
 *    both the shortcut intent extra and the dynamic shortcut's `id`.
 *
 * The IDs are deliberately short and stable across releases - they
 * persist in SharedPreferences (via [RecentToolsRegistry]) and in the
 * launcher's pinned-shortcut store, so renaming one would orphan
 * existing user state.
 */
internal object ToolShortcutCatalog {

    data class Entry(
        val id: String,
        val route: Any,
        val shortLabelRes: Int,
        val longLabelRes: Int,
    )

    private val entries: List<Entry> = listOf(
        Entry("units", UnitConverterRoute, R.string.tool_units, R.string.shortcut_units_long),
        Entry("loan", LoanRoute, R.string.tool_loan, R.string.tool_loan),
        Entry("gst", GstRoute, R.string.tool_gst, R.string.tool_gst),
        Entry("discount", DiscountRoute, R.string.tool_discount, R.string.tool_discount),
        Entry("tipsplit", TipSplitRoute, R.string.tool_tipsplit, R.string.tool_tipsplit),
        Entry("investment", InvestmentRoute, R.string.tool_investment, R.string.tool_investment),
        Entry("percent", PercentRoute, R.string.tool_percent, R.string.tool_percent),
        Entry("bmi", BmiRoute, R.string.tool_bmi, R.string.tool_bmi),
        Entry("age", AgeRoute, R.string.tool_age, R.string.tool_age),
        Entry("datediff", DateDiffRoute, R.string.tool_date_diff, R.string.tool_date_diff),
        Entry("timezone", TimezoneRoute, R.string.tool_timezone, R.string.tool_timezone),
        Entry("ovulation", OvulationRoute, R.string.tool_ovulation, R.string.tool_ovulation),
        Entry("bases", BaseConverterRoute, R.string.tool_basecalc, R.string.tool_basecalc),
        Entry("tape", TapeRoute, R.string.tool_tape, R.string.tool_tape),
    )

    private val byIdMap = entries.associateBy { it.id }
    private val byRouteMap = entries.associateBy { it.route }

    fun byId(id: String): Entry? = byIdMap[id]
    fun idOf(route: Any): String? = byRouteMap[route]?.id
}

