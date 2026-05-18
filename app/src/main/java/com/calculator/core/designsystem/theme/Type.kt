package com.calculator.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * App typography.
 *
 * The default Material 3 type scale is reused, with one customisation: the
 * display variants used by the calculator readout opt in to tabular figures
 * (`tnum` OpenType feature) so digit widths stay constant as the user types.
 * This prevents the result from visually jittering during input.
 *
 * Body and label styles are left untouched - they should respect the user's
 * device font scale per accessibility settings.
 *
 * `tnum` is the OpenType feature tag for tabular figures; passed as a raw
 * string since Compose's `fontFeatureSettings` property is a plain `String`.
 */
private val TabularNumeralStyle =
    TextStyle(
        fontFeatureSettings = "tnum",
    )

private val Default = Typography()

internal val CalculatorTypography: Typography =
    Default.copy(
        displayLarge = Default.displayLarge.merge(TabularNumeralStyle).copy(fontWeight = FontWeight.Medium),
        displayMedium = Default.displayMedium.merge(TabularNumeralStyle),
        displaySmall = Default.displaySmall.merge(TabularNumeralStyle),
        headlineLarge = Default.headlineLarge.merge(TabularNumeralStyle),
        headlineMedium = Default.headlineMedium.merge(TabularNumeralStyle),
        headlineSmall = Default.headlineSmall.merge(TabularNumeralStyle),
    )
