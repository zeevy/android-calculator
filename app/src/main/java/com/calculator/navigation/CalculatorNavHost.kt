package com.calculator.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calculator.feature.basic.ui.BasicCalculatorScreen
import com.calculator.feature.converter.unit.UnitConverterScreen
import com.calculator.feature.datetime.age.AgeScreen
import com.calculator.feature.datetime.datediff.DateDiffScreen
import com.calculator.feature.finance.discount.DiscountScreen
import com.calculator.feature.finance.gst.GstScreen
import com.calculator.feature.finance.loan.LoanScreen
import com.calculator.feature.health.bmi.BmiScreen
import com.calculator.feature.health.ovulation.OvulationScreen

/**
 * Root navigation host.
 *
 * Single-activity convention: this is the only `NavHost` in the app.
 * Features are registered via `composable<RouteType>` blocks below.
 *
 * Navigation actions belong inside individual screens (which receive
 * navigation lambdas from here), keeping route construction in one place
 * and screens unaware of how they are mounted.
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
            "units" -> navController.navigate(UnitConverterRoute)
            else -> Unit // "basic" or null - already at start.
        }
    }

    NavHost(
        navController = navController,
        startDestination = BasicCalculatorRoute,
    ) {
        composable<BasicCalculatorRoute> {
            BasicCalculatorScreen(
                onOpenUnitConverter = { navController.navigate(UnitConverterRoute) },
                onOpenLifeCalc = { route -> navController.navigate(route) },
            )
        }

        composable<UnitConverterRoute> {
            UnitConverterScreen(onUp = navController::popBackStack)
        }

        composable<LoanRoute> { LoanScreen(onUp = navController::popBackStack) }
        composable<GstRoute> { GstScreen(onUp = navController::popBackStack) }
        composable<DiscountRoute> { DiscountScreen(onUp = navController::popBackStack) }
        composable<BmiRoute> { BmiScreen(onUp = navController::popBackStack) }
        composable<AgeRoute> { AgeScreen(onUp = navController::popBackStack) }
        composable<DateDiffRoute> { DateDiffScreen(onUp = navController::popBackStack) }
        composable<OvulationRoute> { OvulationScreen(onUp = navController::popBackStack) }
    }
}
