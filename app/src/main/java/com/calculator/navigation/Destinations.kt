// Holds every navigation destination in one place; the file is deliberately
// not named after a single declaration so future routes can sit alongside.
@file:Suppress("ktlint:standard:filename")

package com.calculator.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations.
 *
 * Each destination is a `@Serializable` Kotlin type, which Navigation
 * Compose 2.8+ accepts directly via `composable<DestType> { ... }` blocks.
 * Compared to string routes this catches typos at compile time and lets
 * arguments travel as typed properties.
 *
 * To add a tool: a route here, one entry in [Tools], one `composable`
 * line in [CalculatorNavHost], and its strings.
 */
sealed interface ToolRoute

/** The basic calculator; the start destination, always at the bottom of the back stack. */
@Serializable
data object BasicCalculatorRoute : ToolRoute

@Serializable
data object UnitConverterRoute : ToolRoute

@Serializable
data object LoanRoute : ToolRoute

@Serializable
data object GstRoute : ToolRoute

@Serializable
data object DiscountRoute : ToolRoute

@Serializable
data object TipSplitRoute : ToolRoute

@Serializable
data object InvestmentRoute : ToolRoute

@Serializable
data object DepositRoute : ToolRoute

@Serializable
data object PercentRoute : ToolRoute

@Serializable
data object BaseConverterRoute : ToolRoute

@Serializable
data object TapeRoute : ToolRoute

@Serializable
data object BmiRoute : ToolRoute

@Serializable
data object AgeRoute : ToolRoute

@Serializable
data object DateDiffRoute : ToolRoute

@Serializable
data object TimezoneRoute : ToolRoute

@Serializable
data object OvulationRoute : ToolRoute
