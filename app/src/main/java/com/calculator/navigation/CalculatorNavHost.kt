package com.calculator.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calculator.feature.basic.ui.BasicCalculatorScreen
import com.calculator.feature.converter.currency.CurrencyConverterScreen
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
 */
@Composable
fun CalculatorNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = BasicCalculatorRoute,
    ) {
        composable<BasicCalculatorRoute> {
            BasicCalculatorScreen(
                onOpenUnitConverter = { navController.navigate(UnitConverterRoute) },
                onOpenCurrencyConverter = { navController.navigate(CurrencyConverterRoute) },
                onOpenLifeCalc = { route -> navController.navigate(route) },
            )
        }

        composable<UnitConverterRoute> {
            UnitConverterScreen(onUp = navController::popBackStack)
        }

        composable<CurrencyConverterRoute> {
            CurrencyConverterScreen(onUp = navController::popBackStack)
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
