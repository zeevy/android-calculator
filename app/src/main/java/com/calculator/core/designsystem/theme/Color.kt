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
 * The palette encodes the app's signature "iOS calculator" identity as a
 * proper Material 3 color scheme so every surface honors light/dark and, when
 * enabled, dynamic color. The keypad maps onto roles like this:
 *
 *   operator keys (/ x - + =)   -> primary / onPrimary
 *   digit keys (0-9 .)          -> secondaryContainer / onSecondaryContainer
 *   function keys (scientific)  -> tertiaryContainer / onTertiaryContainer
 *   modifier keys (AC / +- %)   -> inverseSurface / inverseOnSurface
 *
 * Chrome maps onto: cards -> surfaceContainer, segmented/rows ->
 * surfaceContainerHigh, dim labels -> onSurfaceVariant, accent values ->
 * primary, dividers -> outlineVariant.
 *
 * Tokens follow Material 3 naming so the mapping into `lightColorScheme()`
 * and `darkColorScheme()` stays predictable.
 */

// Signature brand accent (iOS calculator orange). Used as `primary` in both
// fallback schemes; dynamic color replaces it with the wallpaper primary.
internal val BrandOrange = Color(0xFFFF9F0A)

// ----- Light scheme -----
internal val LightPrimary = BrandOrange
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFFFE0B2)
internal val LightOnPrimaryContainer = Color(0xFF2A1800)

// Digit keys.
internal val LightSecondary = Color(0xFF8E8E93)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFD1D1D6)
internal val LightOnSecondaryContainer = Color(0xFF1C1C1E)

// Function (scientific) keys.
internal val LightTertiary = Color(0xFF636366)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFE5E5EA)
internal val LightOnTertiaryContainer = Color(0xFF1C1C1E)

internal val LightBackground = Color(0xFFF2F2F7)
internal val LightOnBackground = Color(0xFF1C1C1E)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightOnSurface = Color(0xFF1C1C1E)
internal val LightSurfaceVariant = Color(0xFFE5E5EA)
internal val LightOnSurfaceVariant = Color(0xFF6C6C70)

internal val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
internal val LightSurfaceContainerLow = Color(0xFFFFFFFF)
internal val LightSurfaceContainer = Color(0xFFFFFFFF)
// High / Highest double as the keypad digit / function key greys.
internal val LightSurfaceContainerHigh = Color(0xFFD1D1D6)
internal val LightSurfaceContainerHighest = Color(0xFFE5E5EA)

// Modifier keys (AC, +/-, %): mid-grey with light text.
internal val LightInverseSurface = Color(0xFF505050)
internal val LightInverseOnSurface = Color(0xFFFFFFFF)

internal val LightOutline = Color(0xFFC6C6C8)
internal val LightOutlineVariant = Color(0xFFE5E5EA)

internal val LightError = Color(0xFFC4302B)
internal val LightOnError = Color(0xFFFFFFFF)

// ----- Dark scheme (preserves the current iOS-dark look) -----
internal val DarkPrimary = BrandOrange
internal val DarkOnPrimary = Color(0xFFFFFFFF)
internal val DarkPrimaryContainer = Color(0xFFB36F00)
internal val DarkOnPrimaryContainer = Color(0xFFFFFFFF)

// Digit keys (iOS dark digit grey).
internal val DarkSecondary = Color(0xFFAEAEB2)
internal val DarkOnSecondary = Color(0xFF1C1C1E)
internal val DarkSecondaryContainer = Color(0xFF505050)
internal val DarkOnSecondaryContainer = Color(0xFFFFFFFF)

// Function (scientific) keys (iOS dark function grey).
internal val DarkTertiary = Color(0xFFC7C7CC)
internal val DarkOnTertiary = Color(0xFF1C1C1E)
internal val DarkTertiaryContainer = Color(0xFF707070)
internal val DarkOnTertiaryContainer = Color(0xFFFFFFFF)

internal val DarkBackground = Color(0xFF000000)
internal val DarkOnBackground = Color(0xFFFFFFFF)
internal val DarkSurface = Color(0xFF000000)
internal val DarkOnSurface = Color(0xFFFFFFFF)
internal val DarkSurfaceVariant = Color(0xFF2C2C2E)
internal val DarkOnSurfaceVariant = Color(0xFFAEAEB2)

internal val DarkSurfaceContainerLowest = Color(0xFF000000)
internal val DarkSurfaceContainerLow = Color(0xFF1C1C1E)
internal val DarkSurfaceContainer = Color(0xFF1C1C1E)
internal val DarkSurfaceContainerHigh = Color(0xFF2C2C2E)
internal val DarkSurfaceContainerHighest = Color(0xFF3A3A3C)

// Modifier keys (AC, +/-, %): light grey with dark text.
internal val DarkInverseSurface = Color(0xFFA5A5A5)
internal val DarkInverseOnSurface = Color(0xFF000000)

internal val DarkOutline = Color(0xFF48484A)
internal val DarkOutlineVariant = Color(0xFF2C2C2E)

internal val DarkError = Color(0xFFFF6961)
internal val DarkOnError = Color(0xFF000000)
