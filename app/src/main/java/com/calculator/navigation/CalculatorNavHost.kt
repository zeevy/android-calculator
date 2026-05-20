package com.calculator.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calculator.feature.basic.ui.BasicCalculatorScreen
import com.calculator.feature.converter.unit.UnitConverterScreen
import com.calculator.feature.datetime.age.AgeScreen
import com.calculator.feature.datetime.datediff.DateDiffScreen
import com.calculator.feature.datetime.timezone.TimezoneScreen
import com.calculator.feature.finance.discount.DiscountScreen
import com.calculator.feature.finance.gst.GstScreen
import com.calculator.feature.finance.investment.InvestmentScreen
import com.calculator.feature.finance.loan.LoanScreen
import com.calculator.feature.finance.tipsplit.TipSplitScreen
import com.calculator.feature.health.bmi.BmiScreen
import com.calculator.feature.health.ovulation.OvulationScreen

/**
 * Root navigation host.
 *
 * Single-activity convention: this is the only `NavHost` in the app.
 * Features are registered via `composable<RouteType>` blocks below.
 *
 * Navigation is driven by a single [openTool] helper - every screen
 * receives the same `onNavigate: (Any) -> Unit` lambda from here, and
 * the helper uses `popUpTo(BasicCalculatorRoute)` so the back stack
 * stays flat: the basic calculator is always the only thing under the
 * current tool, regardless of how the user got there. That matches the
 * UI promise that each tool reads as a standalone page reached via the
 * hamburger menu rather than a stack of forward navigations.
 *
 * @param startDestinationHint Optional launcher-shortcut hint; if set
 *   to a recognised value ("units"), the corresponding screen is
 *   pushed onto the back stack right after the basic calculator so
 *   back returns to the calculator instead of leaving the app.
 */
@Composable
fun CalculatorNavHost(startDestinationHint: String? = null) {
    val navController = rememberNavController()

    // Apply the shortcut destination once, after the nav controller is
    // ready. LaunchedEffect with Unit keys ensures we don't re-navigate
    // on config change.
    androidx.compose.runtime.LaunchedEffect(startDestinationHint) {
        when (startDestinationHint) {
            "units" -> navController.openTool(UnitConverterRoute)
            else -> Unit // "basic" or null - already at start.
        }
    }

    NavHost(
        navController = navController,
        startDestination = BasicCalculatorRoute,
    ) {
        composable<BasicCalculatorRoute> {
            BasicCalculatorScreen(onNavigate = navController::openTool)
        }
        composable<UnitConverterRoute> {
            UnitConverterScreen(onNavigate = navController::openTool)
        }
        composable<LoanRoute> { LoanScreen(onNavigate = navController::openTool) }
        composable<GstRoute> { GstScreen(onNavigate = navController::openTool) }
        composable<DiscountRoute> { DiscountScreen(onNavigate = navController::openTool) }
        composable<TipSplitRoute> { TipSplitScreen(onNavigate = navController::openTool) }
        composable<InvestmentRoute> { InvestmentScreen(onNavigate = navController::openTool) }
        composable<BmiRoute> { BmiScreen(onNavigate = navController::openTool) }
        composable<AgeRoute> { AgeScreen(onNavigate = navController::openTool) }
        composable<DateDiffRoute> { DateDiffScreen(onNavigate = navController::openTool) }
        composable<TimezoneRoute> { TimezoneScreen(onNavigate = navController::openTool) }
        composable<OvulationRoute> { OvulationScreen(onNavigate = navController::openTool) }
    }
}

/**
 * Navigates to [route] while keeping [BasicCalculatorRoute] as the sole
 * underlying entry.
 *
 * Behaviour:
 *  - To BasicCalculatorRoute: pop the stack back down to it (no new
 *    instance pushed) so re-tapping Home from any tool returns to the
 *    same calculator state instead of recreating it.
 *  - To any other route: pop above the basic calculator first, then
 *    push, with `launchSingleTop` so re-tapping the current tool's tile
 *    is a no-op. The user then has BasicCalc + the new tool on the
 *    stack, regardless of how many tools they bounced through to get
 *    there.
 */
internal fun NavController.openTool(route: Any) {
    if (route === BasicCalculatorRoute) {
        // popBackStack to basic calc; if it's already the top, nothing
        // happens (which is the desired no-op).
        popBackStack(BasicCalculatorRoute, inclusive = false)
        return
    }
    navigate(route) {
        popUpTo(BasicCalculatorRoute) { saveState = false }
        launchSingleTop = true
    }
}
