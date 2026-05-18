package com.calculator.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/*
 * Design-system color tokens.
 *
 * These are the **fallback** colors used when Material You dynamic color is
 * unavailable or disabled by the user. On supported devices (API 31+ with
 * dynamic color enabled) the system overrides these with wallpaper-derived
 * palettes - see [CalculatorTheme].
 *
 * Tokens follow Material 3 naming so the mapping into `lightColorScheme()`
 * and `darkColorScheme()` stays predictable.
 */

// ----- Light scheme -----
internal val LightPrimary = Color(0xFF6750A4)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFEADDFF)
internal val LightOnPrimaryContainer = Color(0xFF21005D)

internal val LightSecondary = Color(0xFF625B71)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFE8DEF8)
internal val LightOnSecondaryContainer = Color(0xFF1D192B)

internal val LightTertiary = Color(0xFF7D5260)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFFFD8E4)
internal val LightOnTertiaryContainer = Color(0xFF31111D)

internal val LightBackground = Color(0xFFFFFBFE)
internal val LightOnBackground = Color(0xFF1C1B1F)
internal val LightSurface = Color(0xFFFFFBFE)
internal val LightOnSurface = Color(0xFF1C1B1F)
internal val LightError = Color(0xFFB3261E)
internal val LightOnError = Color(0xFFFFFFFF)

// ----- Dark scheme -----
internal val DarkPrimary = Color(0xFFD0BCFF)
internal val DarkOnPrimary = Color(0xFF381E72)
internal val DarkPrimaryContainer = Color(0xFF4F378B)
internal val DarkOnPrimaryContainer = Color(0xFFEADDFF)

internal val DarkSecondary = Color(0xFFCCC2DC)
internal val DarkOnSecondary = Color(0xFF332D41)
internal val DarkSecondaryContainer = Color(0xFF4A4458)
internal val DarkOnSecondaryContainer = Color(0xFFE8DEF8)

internal val DarkTertiary = Color(0xFFEFB8C8)
internal val DarkOnTertiary = Color(0xFF492532)
internal val DarkTertiaryContainer = Color(0xFF633B48)
internal val DarkOnTertiaryContainer = Color(0xFFFFD8E4)

internal val DarkBackground = Color(0xFF1C1B1F)
internal val DarkOnBackground = Color(0xFFE6E1E5)
internal val DarkSurface = Color(0xFF1C1B1F)
internal val DarkOnSurface = Color(0xFFE6E1E5)
internal val DarkError = Color(0xFFF2B8B5)
internal val DarkOnError = Color(0xFF601410)
