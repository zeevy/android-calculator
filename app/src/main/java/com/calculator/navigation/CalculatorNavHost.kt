package com.calculator.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calculator.feature.basic.ui.BasicCalculatorScreen
import com.calculator.feature.converter.unit.UnitConverterScreen

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
            )
        }

        composable<UnitConverterRoute> {
            UnitConverterScreen(onUp = navController::popBackStack)
        }
    }
}
