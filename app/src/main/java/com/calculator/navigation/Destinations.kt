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
 * New features add a destination here and a corresponding `composable`
 * registration in [CalculatorNavHost].
 */
@Serializable
data object BasicCalculatorRoute

// Future destinations follow the same pattern, e.g.:
// @Serializable data object SettingsRoute
// @Serializable data class CurrencyConverterRoute(val baseCurrency: String? = null)
