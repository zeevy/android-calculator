package com.calculator.feature.basic.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Smallest key height a finger (and Android's accessibility guidance) needs.
internal val MIN_TOUCH_TARGET = 48.dp

// Keypad geometry used by [minKeyHeightDp]: 12dp outer padding on both sides
// plus three 8dp gaps, 8dp between rows, and the space the screen spends
// outside the keypad (system bars, divider, paddings). The display keeps at
// least MIN_DISPLAY_HEIGHT_DP for the result and the menu button.
private const val KEYPAD_SIDE_SPACE_DP = 48f
private const val KEYS_PER_ROW = 4f
private const val KEY_GAP_DP = 8f
private const val SCREEN_CHROME_DP = 113f
private const val MIN_DISPLAY_HEIGHT_DP = 160f
private const val COMPACT_ROWS_BASIC = 2
private const val COMPACT_ROWS_SCIENTIFIC = 6
private const val NORMAL_ROWS = 4

/** Smallest key height for the keypad being drawn; set once per keypad. */
internal val LocalMinKeyHeight = compositionLocalOf { MIN_TOUCH_TARGET }

/**
 * Key height from the row width and [aspectRatio] (width / height), but
 * never below [minHeight], so the short compact rows stay large enough
 * for a finger. `aspectRatio` alone cannot do this: it ignores a minimum
 * height that does not fit its ratio.
 */
internal fun Modifier.keySize(
    aspectRatio: Float,
    minHeight: Dp,
): Modifier =
    layout { measurable, constraints ->
        val width = constraints.maxWidth
        val height = maxOf((width / aspectRatio).roundToInt(), minHeight.roundToPx())
        val placeable = measurable.measure(Constraints.fixed(width, height))
        layout(width, height) { placeable.place(0, 0) }
    }

/**
 * Smallest key height for a screen of [screenWidthDp] x [screenHeightDp], in dp.
 *
 * Keys are at least 48dp ([MIN_TOUCH_TARGET]) while the keypad still leaves
 * the display [MIN_DISPLAY_HEIGHT_DP] of room. At very large display sizes
 * (narrow in dp) the scientific keypad would push the result and the menu
 * off screen, so there the keys keep their plain width ratio instead.
 */
internal fun minKeyHeightDp(
    screenWidthDp: Int,
    screenHeightDp: Int,
    scientific: Boolean,
): Float {
    val floor = MIN_TOUCH_TARGET.value
    val keyWidth = (screenWidthDp - KEYPAD_SIDE_SPACE_DP) / KEYS_PER_ROW
    val compactRows = if (scientific) COMPACT_ROWS_SCIENTIFIC else COMPACT_ROWS_BASIC
    val rows = compactRows + NORMAL_ROWS
    val keypadHeight =
        compactRows * maxOf(keyWidth / BUTTON_ASPECT_RATIO_COMPACT, floor) +
            NORMAL_ROWS * maxOf(keyWidth / BUTTON_ASPECT_RATIO, floor) +
            (rows - 1) * KEY_GAP_DP + SCREEN_CHROME_DP
    return if (screenHeightDp - keypadHeight >= MIN_DISPLAY_HEIGHT_DP) floor else 0f
}
