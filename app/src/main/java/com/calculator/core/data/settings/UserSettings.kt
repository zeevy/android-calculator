package com.calculator.core.data.settings

/**
 * User-facing app settings. Persisted to DataStore Preferences.
 *
 * Defaults are conservative:
 *  - Theme follows the system.
 *  - Dynamic color on (Material You).
 *  - Haptics + sound on (parity with iOS).
 *  - Precision 12 sig figs (matches the math engine's transcendental
 *    round-trip).
 *  - Crash reporting OFF until the user opts in (open-source / privacy).
 */
data class UserSettings(
    val theme: ThemeOption = ThemeOption.System,
    val dynamicColor: Boolean = true,
    val haptics: Boolean = true,
    val sound: Boolean = true,
    val precision: Int = 12,
    val crashOptIn: Boolean = false,
) {
    enum class ThemeOption { System, Light, Dark }
}
