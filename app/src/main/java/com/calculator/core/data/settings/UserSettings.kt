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
    /**
     * Per-tool "remember last selection" preferences.
     *
     * Persisted alongside the rest of the user settings so the next
     * time a tool opens the user lands on what they had picked before,
     * not the hard-coded default. The unit converter already persists
     * its (from, to) pair per category in a separate Room table; this
     * adds the missing category memory plus toggle defaults for the
     * other tools that have a meaningful "which mode were you in"
     * question.
     */
    val lastUnitCategory: String? = null,
    val gstIntraState: Boolean = true,
    val gstRate: String = "18",
    val bmiImperial: Boolean = false,
) {
    enum class ThemeOption { System, Light, Dark }
}
