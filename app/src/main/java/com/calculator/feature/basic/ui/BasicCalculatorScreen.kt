package com.calculator.feature.basic.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calculator.R
import com.calculator.core.designsystem.theme.CalculatorTheme
import com.calculator.core.math.AngleMode
import com.calculator.feature.history.HistorySheetContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Top-level composable for the basic calculator screen.
 *
 * Pulls the [BasicCalculatorViewModel] from Hilt and renders the
 * stateless [BasicCalculatorScreenContent] - this split keeps previews
 * possible without a ViewModel and makes UI tests straightforward.
 */
@Composable
fun BasicCalculatorScreen(
    onOpenUnitConverter: () -> Unit = {},
    onOpenCurrencyConverter: () -> Unit = {},
    onOpenLifeCalc: (Any) -> Unit = {},
    viewModel: BasicCalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BasicCalculatorScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenUnitConverter = onOpenUnitConverter,
        onOpenCurrencyConverter = onOpenCurrencyConverter,
        onOpenLifeCalc = onOpenLifeCalc,
    )
}

/**
 * Stateless screen content.
 *
 * Decoupling state from the ViewModel here is the single most useful
 * thing for testability: previews and Compose UI tests construct a
 * [BasicCalculatorUiState] directly and pass a no-op `onEvent` lambda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BasicCalculatorScreenContent(
    state: BasicCalculatorUiState,
    onEvent: (BasicCalculatorEvent) -> Unit,
    onOpenUnitConverter: () -> Unit = {},
    onOpenCurrencyConverter: () -> Unit = {},
    onOpenLifeCalc: (Any) -> Unit = {},
) {
    var openSheet by remember { mutableStateOf<MenuSheet?>(null) }
    // skipPartiallyExpanded: the sheet jumps straight to its fully-expanded
    // anchor on open instead of stopping at the half-screen detent. All
    // three sheets we host (Tools grid, History list, Settings panel) are
    // functional menus rather than gallery thumbnails - the half-state
    // just hides content and forces the user to swipe up to see it.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val tones = rememberKeyToneGenerator()
    // Settings drives sound and haptics toggles. Falling back to defaults
    // (both on) when the settings VM hasn't emitted yet keeps the UI
    // responsive during the first frame.
    val settingsViewModel: com.calculator.feature.settings.SettingsViewModel = hiltViewModel()
    val userSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val tonesIfEnabled = if (userSettings.sound) tones else null

    CompositionLocalProvider(
        LocalKeyTones provides tonesIfEnabled,
        LocalHapticsEnabled provides userSettings.haptics,
    ) {
        Scaffold(
            // Hand out insets per-child: outer Column absorbs the status-bar
            // inset (so the display card sits below the system icons), and
            // the keypad card absorbs the navigation-bar inset on its bottom
            // edge (so the bottom row of keys doesn't collide with the
            // gesture pill).
            contentWindowInsets = WindowInsets(0.dp),
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp),
            ) {
                // Display sits directly on the screen background - no card,
                // no border. The typographic hierarchy inside (small muted
                // expression line, big bold result) carries the visual
                // weight on its own, and the gap before the keypad tray
                // does the section separation.
                DisplaySection(
                    state = state,
                    onEvent = onEvent,
                    onOpenMenu = { openSheet = MenuSheet.Tools },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )
                Spacer(Modifier.size(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                )
                Spacer(Modifier.size(12.dp))
                // No keypad tray. iOS calculator puts keys directly on the
                // screen background; the colored keys carry the visual
                // structure on their own.
                Keypad(
                    scientific = state.scientific,
                    onEvent = onEvent,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 12.dp),
                )
            }
        }

        if (openSheet != null) {
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { openSheet = null },
                // iOS palette: near-black sheet so it sits flat against the
                // calculator's black canvas, with a light-grey drag handle
                // to match the modifier-key tone.
                containerColor = IosSheetBackground,
                // contentColor is intentionally forced to white. The sheet
                // background is hardcoded dark regardless of the system
                // theme, so we can't let Material derive contentColor from
                // the theme - in light mode it would resolve to a black
                // onSurface and titles like "History" would render black
                // on the dark sheet (effectively invisible).
                contentColor = Color.White,
                dragHandle = {
                    BottomSheetDefaults.DragHandle(
                        color = IosKeyModifierContainer,
                    )
                },
            ) {
                when (openSheet) {
                    MenuSheet.Tools ->
                        ToolsSheetContent(
                            state = state,
                            onEvent = onEvent,
                            onOpenHistory = { openSheet = MenuSheet.History },
                            onOpenSettings = { openSheet = MenuSheet.Settings },
                            onOpenUnitConverter = {
                                scope.launch { sheetState.hide() }
                                openSheet = null
                                onOpenUnitConverter()
                            },
                            onOpenCurrencyConverter = {
                                scope.launch { sheetState.hide() }
                                openSheet = null
                                onOpenCurrencyConverter()
                            },
                            onOpenLifeCalc = { route ->
                                scope.launch { sheetState.hide() }
                                openSheet = null
                                onOpenLifeCalc(route)
                            },
                            onClose = {
                                scope.launch { sheetState.hide() }
                                openSheet = null
                            },
                        )
                    MenuSheet.History ->
                        HistorySheetContent(
                            onReuseExpression = { expr ->
                                onEvent(BasicCalculatorEvent.Clear)
                                onEvent(BasicCalculatorEvent.Append(expr))
                            },
                            onClose = {
                                scope.launch { sheetState.hide() }
                                openSheet = null
                            },
                        )
                    MenuSheet.Settings ->
                        com.calculator.feature.settings.SettingsSheetContent(
                            onClose = {
                                scope.launch { sheetState.hide() }
                                openSheet = null
                            },
                        )
                    null -> Unit
                }
            }
        }
    } // CompositionLocalProvider
}

/** Which sheet is currently displayed in the modal bottom sheet. */
private enum class MenuSheet { Tools, History, Settings }

/**
 * App-level tools sheet, summoned from the top-right hamburger button.
 *
 * Modelled on the Mi Calculator tools menu: a grid of icon + label
 * tiles, one tap to jump to a mode/tool. The currently-active mode is
 * highlighted with the primary container colour.
 *
 * Today the working tiles are Basic and Advanced (scientific). The rest
 * are placeholders for upcoming features (history in Phase 3, converters
 * in Phase 5/6, life calculators in Phase 7) and are tappable but flash
 * a "coming soon" hint to avoid silently swallowing user intent.
 *
 * Each lambda routes to a distinct destination; collapsing them into a
 * single sealed event would push the routing decision into the screen,
 * hence the suppression of [LongParameterList].
 */
@Suppress("LongParameterList")
@Composable
private fun ToolsSheetContent(
    state: BasicCalculatorUiState,
    onEvent: (BasicCalculatorEvent) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUnitConverter: () -> Unit,
    onOpenCurrencyConverter: () -> Unit,
    onOpenLifeCalc: (Any) -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        val tiles =
            listOf(
                ToolTile(
                    icon = Icons.Filled.Calculate,
                    label = stringResource(R.string.tool_basic),
                    enabled = true,
                    selected = !state.scientific,
                    onTap = {
                        if (state.scientific) onEvent(BasicCalculatorEvent.ToggleScientific)
                        onClose()
                    },
                ),
                ToolTile(
                    icon = Icons.Filled.Functions,
                    label = stringResource(R.string.tool_advanced),
                    enabled = true,
                    selected = state.scientific,
                    onTap = {
                        if (!state.scientific) onEvent(BasicCalculatorEvent.ToggleScientific)
                        onClose()
                    },
                ),
                ToolTile(
                    icon = Icons.Filled.History,
                    label = stringResource(R.string.tool_history),
                    enabled = true,
                    selected = false,
                    onTap = onOpenHistory,
                ),
                ToolTile(
                    icon = Icons.Filled.Straighten,
                    label = stringResource(R.string.tool_units),
                    enabled = true,
                    selected = false,
                    onTap = onOpenUnitConverter,
                ),
                ToolTile(
                    icon = Icons.Filled.CurrencyExchange,
                    label = stringResource(R.string.tool_currency),
                    enabled = true,
                    selected = false,
                    onTap = onOpenCurrencyConverter,
                ),
                ToolTile(
                    icon = Icons.Filled.AccountBalance,
                    label = stringResource(R.string.tool_loan),
                    enabled = true,
                    selected = false,
                    onTap = { onOpenLifeCalc(com.calculator.navigation.LoanRoute) },
                ),
                ToolTile(
                    icon = Icons.Filled.Receipt,
                    label = stringResource(R.string.tool_gst),
                    enabled = true,
                    selected = false,
                    onTap = { onOpenLifeCalc(com.calculator.navigation.GstRoute) },
                ),
                ToolTile(
                    icon = Icons.Filled.LocalOffer,
                    label = stringResource(R.string.tool_discount),
                    enabled = true,
                    selected = false,
                    onTap = { onOpenLifeCalc(com.calculator.navigation.DiscountRoute) },
                ),
                ToolTile(
                    icon = Icons.Filled.MonitorWeight,
                    label = stringResource(R.string.tool_bmi),
                    enabled = true,
                    selected = false,
                    onTap = { onOpenLifeCalc(com.calculator.navigation.BmiRoute) },
                ),
                ToolTile(
                    icon = Icons.Filled.Cake,
                    label = stringResource(R.string.tool_age),
                    enabled = true,
                    selected = false,
                    onTap = { onOpenLifeCalc(com.calculator.navigation.AgeRoute) },
                ),
                ToolTile(
                    icon = Icons.Filled.DateRange,
                    label = stringResource(R.string.tool_date_diff),
                    enabled = true,
                    selected = false,
                    onTap = { onOpenLifeCalc(com.calculator.navigation.DateDiffRoute) },
                ),
                ToolTile(
                    icon = Icons.Filled.Favorite,
                    label = stringResource(R.string.tool_ovulation),
                    enabled = true,
                    selected = false,
                    onTap = { onOpenLifeCalc(com.calculator.navigation.OvulationRoute) },
                ),
                ToolTile(
                    icon = Icons.Filled.Settings,
                    label = stringResource(R.string.tool_settings),
                    enabled = true,
                    selected = false,
                    onTap = onOpenSettings,
                ),
            )

        tiles.chunked(TOOL_TILE_COLUMNS).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowTiles.forEach { tile ->
                    ToolTileButton(tile = tile, modifier = Modifier.weight(1f))
                }
                // Pad the trailing slot(s) so partial rows keep tile size.
                repeat(TOOL_TILE_COLUMNS - rowTiles.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.size(8.dp))
        }

        Spacer(Modifier.size(16.dp))
    }
}

/** A single tile (icon + label) inside the [SettingsSheetContent] grid. */
private data class ToolTile(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val enabled: Boolean,
    val selected: Boolean,
    val onTap: () -> Unit,
)

@Composable
private fun ToolTileButton(tile: ToolTile, modifier: Modifier = Modifier) {
    // iOS palette: selected tile uses the same orange as the operator
    // keys; enabled tiles use the digit-key dark grey; disabled tiles
    // fade toward the function-key medium grey at 60% alpha so they
    // read as placeholders without disappearing entirely.
    val containerColor =
        when {
            tile.selected -> IosKeyOperator
            tile.enabled -> IosKeyDigitContainer
            else -> IosKeyDigitContainer.copy(alpha = 0.6f)
        }
    val contentColor =
        when {
            tile.selected -> Color.White
            tile.enabled -> Color.White
            else -> Color.White.copy(alpha = 0.5f)
        }
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(20.dp))
                .background(containerColor)
                .clickable(enabled = tile.enabled, onClick = tile.onTap)
                .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = tile.icon,
            contentDescription = tile.label,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = tile.label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
        )
    }
}

private const val TOOL_TILE_COLUMNS = 3

/**
 * Display section: expression + live preview bottom-right, mode chips
 * top-left, hamburger icon top-right. Implemented as a Box so each
 * piece sits exactly where it should without padding rows wasting space.
 */
@Composable
private fun DisplaySection(
    state: BasicCalculatorUiState,
    onEvent: (BasicCalculatorEvent) -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Display(
            expression = state.expression,
            preview = state.liveResult,
            error = state.errorMessage,
            lastCommittedExpression = state.lastCommittedExpression,
            lastValidPreview = state.lastValidPreview,
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp)
                    .padding(top = 48.dp),
        )

        Row(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.scientific) {
                AssistChip(
                    onClick = { onEvent(BasicCalculatorEvent.ToggleAngleMode) },
                    label = { Text(if (state.angleMode == AngleMode.Degree) "DEG" else "RAD") },
                )
            }
            if (state.memory != BigDecimal.ZERO) {
                AssistChip(
                    onClick = { onEvent(BasicCalculatorEvent.MemoryRecall) },
                    label = { Text("M") },
                    colors =
                        AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                )
            }
        }

        // Hamburger: status-bar inset keeps it below the system icons, then
        // a 40dp box (vs IconButton's default 48dp) trims the visible gap
        // between status bar and glyph from ~12dp to ~8dp.
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(end = 4.dp, top = 4.dp)
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable(onClick = onOpenMenu),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = stringResource(R.string.basic_open_menu),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Result display: always two lines.
 *
 *  - **Top line** (smaller, muted): the *input* - either the just-
 *    committed expression after `=`, or the expression being typed
 *    when there's a live preview to show below. Empty otherwise (but
 *    the layout slot is still allocated so the bottom line never
 *    shifts mid-typing).
 *  - **Bottom line** (large bold): the *result* - either the live
 *    preview while typing, the canonical result after `=`, the error
 *    message (red), or `0` as a default when the calculator is empty.
 *
 * Both lines are always rendered so the display never "switches"
 * between one-line and two-line layouts as the user types.
 *
 * State → (top, bottom) mapping:
 *
 *     | Calculator state              | Top line       | Bottom line       |
 *     |-------------------------------|----------------|-------------------|
 *     | Empty / just digits ("5")     | (blank)        | expression / "0"  |
 *     | Mid-expression ("5+3")        | expression     | liveResult        |
 *     | Post-equals ("5+3=8")         | committed expr | expression (8)    |
 *     | After-= chain ("8+2")         | expression     | liveResult        |
 *     | Error                         | expression     | error (red)       |
 */
@Composable
private fun Display(
    expression: String,
    preview: String?,
    error: String?,
    lastCommittedExpression: String?,
    lastValidPreview: String?,
    modifier: Modifier = Modifier,
) {
    // Top (input) line: always the expression, with the committed-
    // expression preferred when it's set (i.e. after `=`). We do NOT
    // gate this on `preview != null` because the live preview can
    // disappear mid-typing - typing the next operator (`5+3` -> `5+3+`)
    // makes the preview unevaluable, and gating on it would blank the
    // top line on every operator press. Showing the expression
    // unconditionally keeps the layout stable across every keystroke.
    val topText: String = lastCommittedExpression ?: expression
    // Bottom (result) line: result only - never the expression itself.
    // Priority order:
    //   1. error message (red)
    //   2. live preview from the current expression
    //   3. lastValidPreview - the most recent computable result,
    //      held across non-evaluable intermediate states so typing
    //      `5+3` → `5+3+` keeps showing `8` instead of blanking
    //   4. "0" if the calculator is at rest (expression blank and
    //      nothing held)
    //   5. "" otherwise - shouldn't happen in practice because
    //      lastValidPreview persists whenever the expression isn't
    //      blank, but a defensive fallback keeps the line non-null
    val bottomText: String =
        error
            ?: preview
            ?: lastValidPreview
            ?: if (expression.isBlank()) "0" else ""
    val bottomColor =
        if (error != null) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val bottomFontWeight =
        if (error != null) FontWeight.SemiBold else FontWeight.Bold
    val bottomBaseStyle =
        if (error != null) {
            MaterialTheme.typography.displaySmall
        } else {
            MaterialTheme.typography.displayLarge
        }

    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            // Top line always rendered (even when blank) so the bottom
            // line doesn't bounce between vertical positions as the user
            // moves between "fresh digit" and "expression with preview"
            // states.
            AutoSizeText(
                text = topText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxFontSize = MaterialTheme.typography.headlineSmall.fontSize,
                minFontSize = DISPLAY_EXPRESSION_MIN_SIZE,
                maxLines = DISPLAY_EXPRESSION_MAX_LINES,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(4.dp))
            // Bottom line: the dominant element. Style flips to the
            // error palette + slightly smaller ramp when an error is
            // showing so the user sees the message comfortably.
            //
            // Fixed font size (no auto-shrink). If the text would overflow
            // two lines at the base size, Compose clips with an ellipsis
            // at the trailing edge - by design, per user preference, to
            // keep the typography rhythm stable across every keystroke
            // instead of having the result jiggle smaller as digits grow.
            Text(
                text = bottomText,
                style = bottomBaseStyle.copy(fontWeight = bottomFontWeight),
                color = bottomColor,
                maxLines = DISPLAY_RESULT_MAX_LINES,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Right-aligned auto-sizing text. Wraps to up to [maxLines] lines and
 * scales the font size down only when even the full line count would
 * overflow the available width. Measured once per
 * (text, width, lines) tuple via a [TextMeasurer] so there's no
 * recompose loop.
 *
 * Strategy:
 *  1. Try the max font size with up to maxLines of soft-wrapped text.
 *  2. If that still overflows width OR needs more than maxLines, drop
 *     [AUTO_SIZE_STEP_SP] and re-measure.
 *  3. Stop at [minFontSize] - if the string still doesn't fit there,
 *     the trailing characters clip (the alternative is illegible 8sp
 *     text, which is worse than clipping).
 *
 * Each parameter maps to a distinct visual lever (size bounds, color,
 * line cap, modifier) that callers tweak independently, hence the
 * suppression of [LongParameterList].
 */
@Suppress("LongParameterList")
@Composable
private fun AutoSizeText(
    text: String,
    style: TextStyle,
    color: Color,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    maxLines: Int = 1,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        val maxWidthPx = constraints.maxWidth
        // The display box also has a bounded height (in scientific mode
        // the keypad takes most of the screen, leaving only ~130dp for
        // the display). Pass maxHeight into the measurer too so the
        // shrink loop terminates when text overflows *either* axis,
        // not just width. Without this, three lines of displayLarge
        // would overflow the box vertically and crowd the keypad.
        val maxHeightPx =
            if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
        val measureConstraints = Constraints(maxWidth = maxWidthPx, maxHeight = maxHeightPx)
        val fontSize =
            remember(text, maxWidthPx, maxHeightPx, maxLines, maxFontSize, minFontSize, style) {
                var candidate = maxFontSize
                while (candidate.value > minFontSize.value) {
                    val measured =
                        measurer.measure(
                            text = AnnotatedString(text),
                            style = style.copy(fontSize = candidate),
                            maxLines = maxLines,
                            softWrap = maxLines > 1,
                            constraints = measureConstraints,
                        )
                    if (!measured.didOverflowWidth && !measured.didOverflowHeight) break
                    candidate = (candidate.value - AUTO_SIZE_STEP_SP).sp
                }
                candidate
            }
        Text(
            text = text,
            style = style.copy(fontSize = fontSize),
            color = color,
            maxLines = maxLines,
            softWrap = maxLines > 1,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val AUTO_SIZE_STEP_SP = 2f

// Floor for the muted expression-history line. Small so the shrink
// loop can actually find a size that fits in advanced mode (where the
// 9-row keypad leaves only ~100dp for the entire display). The result
// line below does not shrink - it renders at a fixed displayLarge size
// and ellipsizes if the value doesn't fit in two lines.
private val DISPLAY_EXPRESSION_MIN_SIZE = 10.sp

// Two lines apiece. The result line stays at its base font size and
// ellipsizes if the value can't fit in two lines (rare in practice).
// The expression-history line above shrinks instead of ellipsizing so
// the user can always see the whole expression they've typed. Both
// rows share the same vertical budget, so the keypad position doesn't
// shift between modes.
private const val DISPLAY_RESULT_MAX_LINES = 2
private const val DISPLAY_EXPRESSION_MAX_LINES = 2

/**
 * Keypad. Always shows the 4-column basic grid; in scientific mode the
 * trig/log/power/memory rows stack above the basic rows.
 *
 * Sizing strategy: every key is a fixed-aspect-ratio rectangle (width
 * derived from the 4-column grid, height = width / [BUTTON_ASPECT_RATIO]).
 * The keypad therefore wraps its own height, and the display above it
 * gets `weight(1f)` to absorb whatever's left. This is what keeps buttons
 * looking like proper horizontal rectangles instead of squares or tall
 * vertical bars, regardless of how many rows the current mode has.
 *
 * Portrait is enforced by the manifest, so no landscape branch lives
 * here - foldables / multi-window get the same vertical stacking.
 */
@Composable
private fun Keypad(
    scientific: Boolean,
    onEvent: (BasicCalculatorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The standalone Clear (C) key is gone. Long-pressing the backspace
    // key clears the whole expression - same gesture as a physical
    // calculator's "Clear" press-and-hold on the C/CE key.
    //
    // Memory keys live in basicRows so they show in both modes. They
    // are flagged compact, along with the scientific function rows, so
    // those rows render shorter than the digit/operator rows - giving
    // the display more vertical room in advanced mode without changing
    // the basic layout's proportions much.
    val memoryRow =
        KeypadRowSpec(
            keys = listOf(Key.MemoryClear, Key.MemoryRecall, Key.MemoryAdd, Key.MemorySubtract),
            compact = true,
        )
    // Basic-mode modifier row: sign-flip + factorial + percent + divide.
    // Parens are dropped from basic mode (they live in the scientific
    // section in advanced mode); ± and x! take their slots.
    val signFlipRow =
        KeypadRowSpec(
            keys = listOf(Key.SignFlip, Key.Factorial, Key.Symbol("%"), Key.Symbol("÷")),
            compact = true,
        )
    val basicRows =
        listOf(
            memoryRow,
            signFlipRow,
            KeypadRowSpec(listOf(Key.Symbol("7"), Key.Symbol("8"), Key.Symbol("9"), Key.Symbol("×"))),
            KeypadRowSpec(listOf(Key.Symbol("4"), Key.Symbol("5"), Key.Symbol("6"), Key.Symbol("-"))),
            KeypadRowSpec(listOf(Key.Symbol("1"), Key.Symbol("2"), Key.Symbol("3"), Key.Symbol("+"))),
            KeypadRowSpec(listOf(Key.Backspace, Key.Symbol("0"), Key.Symbol("."), Key.Equals)),
        )
    // Four scientific rows, all compact. Parens slot into row 3
    // (replacing π/e there); π and e move to row 4 alongside x² and x³
    // shortcuts so every cell in advanced mode does something useful -
    // no empty placeholders.
    val scientificRows =
        listOf(
            KeypadRowSpec(
                listOf(Key.Function("sin"), Key.Function("cos"), Key.Function("tan"), Key.Function("sqrt", "√")),
                compact = true,
            ),
            KeypadRowSpec(
                listOf(Key.Function("asin", "sin⁻¹"), Key.Function("acos", "cos⁻¹"), Key.Function("atan", "tan⁻¹"), Key.Symbol("^")),
                compact = true,
            ),
            KeypadRowSpec(
                listOf(Key.Function("log"), Key.Function("ln"), Key.LeftParen, Key.RightParen),
                compact = true,
            ),
            KeypadRowSpec(
                listOf(Key.Symbol("π"), Key.Symbol("e"), Key.Squared, Key.Cubed),
                compact = true,
            ),
        )

    KeypadGrid(
        rows = if (scientific) scientificRows + basicRows else basicRows,
        modifier = modifier,
        onEvent = onEvent,
    )
}

/** A keypad row + per-row layout hints. [compact] uses a wider aspect ratio so the row is shorter. */
private data class KeypadRowSpec(val keys: List<Key>, val compact: Boolean = false)

@Composable
private fun KeypadGrid(
    rows: List<KeypadRowSpec>,
    modifier: Modifier,
    onEvent: (BasicCalculatorEvent) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { spec ->
            KeypadRow(
                row = spec.keys,
                aspectRatio =
                    if (spec.compact) BUTTON_ASPECT_RATIO_COMPACT else BUTTON_ASPECT_RATIO,
                compact = spec.compact,
                modifier = Modifier.fillMaxWidth(),
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun KeypadRow(
    row: List<Key>,
    aspectRatio: Float,
    compact: Boolean,
    modifier: Modifier,
    onEvent: (BasicCalculatorEvent) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        row.forEach { key ->
            if (key is Key.Empty) {
                // Reserve the slot so the row's remaining buttons stay
                // the same width as a fully-populated row.
                Spacer(modifier = Modifier.weight(1f).aspectRatio(aspectRatio))
            } else {
                KeyButton(
                    key = key,
                    onEvent = onEvent,
                    compact = compact,
                    modifier =
                        Modifier
                            .weight(1f)
                            .aspectRatio(aspectRatio),
                )
            }
        }
    }
}

/**
 * Sealed key descriptor.
 *
 * Each variant knows how to render itself and which [BasicCalculatorEvent]
 * to dispatch, so the [KeypadRow] does not need a `when` over every label.
 */
private sealed interface Key {
    val label: String

    /** A character key (digit, operator, parenthesis, constant). */
    data class Symbol(
        override val label: String,
    ) : Key

    /**
     * A named function key. Tapping the key appends `<keyword>(` to the
     * expression; the closing `)` is the user's responsibility (or comes
     * from auto-close on `=`).
     */
    data class Function(
        val keyword: String,
        override val label: String = keyword,
    ) : Key

    data object LeftParen : Key {
        override val label: String = "("
    }

    data object RightParen : Key {
        override val label: String = ")"
    }

    data object Clear : Key {
        override val label: String = "C"
    }

    data object Backspace : Key {
        override val label: String = "⌫"
    }

    data object Equals : Key {
        override val label: String = "="
    }

    data object MemoryClear : Key {
        override val label: String = "MC"
    }

    data object MemoryRecall : Key {
        override val label: String = "MR"
    }

    data object MemoryAdd : Key {
        override val label: String = "M+"
    }

    data object MemorySubtract : Key {
        override val label: String = "M-"
    }

    /** Toggle the sign of the trailing operand (`5` <-> `-5`). */
    data object SignFlip : Key {
        override val label: String = "±"
    }

    /** Postfix factorial. Appends `!` to the expression. */
    data object Factorial : Key {
        override val label: String = "x!"
    }

    /** Squares the current operand. Appends `^2` to the expression. */
    data object Squared : Key {
        override val label: String = "x²"
    }

    /** Cubes the current operand. Appends `^3` to the expression. */
    data object Cubed : Key {
        override val label: String = "x³"
    }

    /** Renders as empty space; used to pad partial rows in the keypad grid. */
    data object Empty : Key {
        override val label: String = ""
    }
}

// Dispatcher for the keypad: one `when` arm per `Key` subtype. Adding a
// dispatcher class to split this purely for the linter would only obscure
// the table-style mapping from key to event.
@Suppress("CyclomaticComplexMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyButton(
    key: Key,
    onEvent: (BasicCalculatorEvent) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val tones = LocalKeyTones.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val rawClick: () -> Unit = {
        when (key) {
            is Key.Symbol -> onEvent(BasicCalculatorEvent.Append(key.label))
            is Key.Function -> onEvent(BasicCalculatorEvent.Append("${key.keyword}("))
            Key.LeftParen -> onEvent(BasicCalculatorEvent.Append("("))
            Key.RightParen -> onEvent(BasicCalculatorEvent.Append(")"))
            Key.Clear -> onEvent(BasicCalculatorEvent.Clear)
            Key.Backspace -> onEvent(BasicCalculatorEvent.Backspace)
            Key.Equals -> onEvent(BasicCalculatorEvent.Equals)
            Key.MemoryClear -> onEvent(BasicCalculatorEvent.MemoryClear)
            Key.MemoryRecall -> onEvent(BasicCalculatorEvent.MemoryRecall)
            Key.MemoryAdd -> onEvent(BasicCalculatorEvent.MemoryAdd)
            Key.MemorySubtract -> onEvent(BasicCalculatorEvent.MemorySubtract)
            Key.SignFlip -> onEvent(BasicCalculatorEvent.SignFlip)
            // Factorial is a plain char append - the tokenizer turns
            // trailing `!` into Token.Factorial during evaluation.
            Key.Factorial -> onEvent(BasicCalculatorEvent.Append("!"))
            Key.Squared -> onEvent(BasicCalculatorEvent.Append("^2"))
            Key.Cubed -> onEvent(BasicCalculatorEvent.Append("^3"))
            Key.Empty -> Unit
        }
    }
    // Every click plays its DTMF tone (if sound is enabled in settings)
    // and fires a light haptic tick (if haptics are enabled). Both are
    // silently no-op if disabled or unavailable.
    val click: () -> Unit = {
        tones?.play(key)
        if (hapticsEnabled) {
            haptics.performHapticFeedback(
                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
            )
        }
        rawClick()
    }

    // Long-press wiring. Only Backspace defines a long-press action - it
    // clears the whole expression, in place of the standalone C key.
    val longClick: (() -> Unit)? =
        when (key) {
            Key.Backspace -> {
                { onEvent(BasicCalculatorEvent.Clear) }
            }
            else -> null
        }

    // Keys that auto-repeat while held: digits, dot, parens, arithmetic
    // operators. Backspace deliberately does not repeat (its long-press
    // is bound to Clear) and equals / memory / function keys are
    // one-shot actions where a repeat doesn't make sense.
    val repeatOnHold = when (key) {
        is Key.Symbol -> true
        Key.LeftParen, Key.RightParen -> true
        else -> false
    }

    val category = keyCategoryOf(key)
    val keyShape = RoundedCornerShape(20.dp)

    // iOS-style palette: dark-grey digits, light-grey modifiers (with
    // black text), vivid orange operators and equals (white text).
    // Sourced from the named constants below so the popup sheet and any
    // future surfaces can share the same palette.
    val containerColor =
        when (category) {
            KeyCategory.Digit -> IosKeyDigitContainer
            KeyCategory.Function -> IosKeyFunctionContainer
            KeyCategory.Modifier -> IosKeyModifierContainer
            KeyCategory.Operator, KeyCategory.Equals -> IosKeyOperator
        }
    val contentColor =
        when (category) {
            KeyCategory.Digit, KeyCategory.Function -> Color.White
            KeyCategory.Modifier -> Color.Black
            KeyCategory.Operator, KeyCategory.Equals -> Color.White
        }

    // Operators and equals get the larger display ramp so the action keys
    // read as visually heavier than digits; functions and memory use a
    // slightly tighter ramp because their labels are longer (sin⁻¹, M+).
    // In compact rows (height ~33dp), the displaySmall ramp would
    // exceed the button height and clip - so every category drops one
    // step down in the type ramp when compact is true. This is what
    // fixes "π" and "e" being half-cut: those keys are operator-color
    // but live in a compact row.
    val labelStyle =
        if (compact) {
            when (category) {
                KeyCategory.Operator, KeyCategory.Equals -> MaterialTheme.typography.titleLarge
                KeyCategory.Function -> MaterialTheme.typography.titleMedium
                KeyCategory.Modifier -> MaterialTheme.typography.titleLarge
                KeyCategory.Digit -> MaterialTheme.typography.titleLarge
            }
        } else {
            when (category) {
                KeyCategory.Operator, KeyCategory.Equals -> MaterialTheme.typography.displaySmall
                KeyCategory.Function -> MaterialTheme.typography.titleLarge
                KeyCategory.Modifier -> MaterialTheme.typography.headlineSmall
                KeyCategory.Digit -> MaterialTheme.typography.headlineLarge
            }
        }
    val labelWeight = if (category == KeyCategory.Equals) FontWeight.Bold else FontWeight.Medium

    // Three click modes:
    //   - repeatOnHold (digits, dot, parens, operators): immediate first
    //     fire on press, then auto-repeat after 400ms while still held.
    //   - longClick != null (backspace): tap to delete, long-press to
    //     clear the whole expression.
    //   - everything else (equals, functions, memory): plain click.
    //
    // Box rather than Material Button because Button exposes neither
    // long-press nor repeat. The visual press feedback for repeating
    // keys is the digit appearing in the expression - acceptable
    // tradeoff for the gesture.
    val clickModifier =
        when {
            repeatOnHold -> Modifier.repeatingClickable(onClick = click)
            longClick != null ->
                Modifier.combinedClickable(onClick = click, onLongClick = longClick)
            else -> Modifier.clickable(onClick = click)
        }
    Box(
        modifier =
            modifier
                .clip(keyShape)
                .background(containerColor)
                .then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key.label,
            style = labelStyle.copy(fontWeight = labelWeight),
            color = contentColor,
        )
    }
}

/**
 * Auto-repeating clickable. Fires [onClick] immediately on press, then
 * repeats every [intervalMillis] after an [initialDelayMillis] hold,
 * stopping on release or cancel.
 *
 * This is what makes holding a digit key emit "555555..." the way a
 * physical keyboard's auto-repeat works. The 400ms / 80ms defaults
 * mirror Android's IME long-press behaviour so it feels familiar.
 */
@Composable
private fun Modifier.repeatingClickable(
    enabled: Boolean = true,
    initialDelayMillis: Long = 400L,
    intervalMillis: Long = 80L,
    onClick: () -> Unit,
): Modifier {
    val currentOnClick by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    // The pointerInput sets `pressed` on down and clears it on up/cancel.
    // The LaunchedEffect watches that flag - delay first (so a quick tap
    // doesn't trigger any repeats), then fire onClick on an interval as
    // long as the finger is still down.
    LaunchedEffect(pressed) {
        if (pressed && enabled) {
            delay(initialDelayMillis)
            while (pressed) {
                currentOnClick()
                delay(intervalMillis)
            }
        }
    }
    return this.pointerInput(enabled) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            if (!enabled) return@awaitEachGesture
            currentOnClick() // immediate first click
            pressed = true
            waitForUpOrCancellation()
            pressed = false
        }
    }
}

/**
 * DTMF tone generator for the keypad.
 *
 * Maps digits to the standard phone touch-tones (so 7 sounds like 7
 * and 9 sounds like 9), and gives operators, equals, and modifier keys
 * their own tones so the audio cues differentiate by key category, not
 * just by pitch. Falls back silently if the system refuses to allocate
 * a [ToneGenerator] (some OEM devices restrict STREAM_DTMF).
 */
private class KeyToneGenerator(private val tg: ToneGenerator) {
    fun play(key: Key) {
        val tone = toneFor(key) ?: return
        // 80ms is short enough that overlapping repeats don't smear into
        // one long buzz when a digit auto-repeats.
        runCatching { tg.startTone(tone, KEY_TONE_DURATION_MS) }
    }

    private fun toneFor(key: Key): Int? =
        when (key) {
            is Key.Symbol -> {
                val label = key.label
                when {
                    label.length == 1 && label[0].isDigit() ->
                        ToneGenerator.TONE_DTMF_0 + (label[0] - '0')
                    label == "." -> ToneGenerator.TONE_DTMF_P
                    else -> ToneGenerator.TONE_PROP_BEEP
                }
            }
            Key.Equals -> ToneGenerator.TONE_PROP_ACK
            Key.Backspace -> ToneGenerator.TONE_PROP_PROMPT
            Key.LeftParen, Key.RightParen -> ToneGenerator.TONE_PROP_BEEP
            Key.Clear -> ToneGenerator.TONE_PROP_NACK
            Key.MemoryClear, Key.MemoryRecall, Key.MemoryAdd, Key.MemorySubtract ->
                ToneGenerator.TONE_PROP_BEEP
            Key.SignFlip, Key.Factorial, Key.Squared, Key.Cubed -> ToneGenerator.TONE_PROP_BEEP
            Key.Empty -> null
            is Key.Function -> ToneGenerator.TONE_PROP_BEEP
        }

    fun release() {
        runCatching { tg.release() }
    }
}

private const val KEY_TONE_DURATION_MS = 80
private const val KEY_TONE_VOLUME = 80

/**
 * Constructs and remembers a [KeyToneGenerator] scoped to the current
 * composition. Released automatically on dispose. Returns null if the
 * device wouldn't give us a tone generator (we then play no audio at
 * all rather than crashing).
 */
@Composable
private fun rememberKeyToneGenerator(): KeyToneGenerator? {
    val generator =
        remember {
            runCatching {
                ToneGenerator(AudioManager.STREAM_DTMF, KEY_TONE_VOLUME)
            }.getOrNull()
        }
    val keyTones = remember(generator) { generator?.let(::KeyToneGenerator) }
    DisposableEffect(keyTones) {
        onDispose { keyTones?.release() }
    }
    return keyTones
}

/** CompositionLocal so any key in the tree can play tones without prop drilling. */
private val LocalKeyTones = compositionLocalOf<KeyToneGenerator?> { null }

/**
 * Whether key-press haptic feedback should fire. Defaults to true so
 * existing code paths and previews continue to vibrate.
 */
private val LocalHapticsEnabled = compositionLocalOf { true }

private enum class KeyCategory { Digit, Operator, Modifier, Function, Equals }

private fun keyCategoryOf(key: Key): KeyCategory =
    when (key) {
        Key.Equals -> KeyCategory.Equals
        Key.Clear, Key.LeftParen, Key.RightParen, Key.Backspace, Key.SignFlip -> KeyCategory.Modifier
        Key.MemoryClear, Key.MemoryRecall, Key.MemoryAdd, Key.MemorySubtract -> KeyCategory.Function
        Key.Factorial, Key.Squared, Key.Cubed -> KeyCategory.Function
        Key.Empty -> KeyCategory.Digit // unused; Empty is rendered separately
        is Key.Function -> KeyCategory.Function
        is Key.Symbol ->
            if (key.label in OperatorLabels) KeyCategory.Operator else KeyCategory.Digit
    }

/**
 * Labels of arithmetic-operator keys that get the operator (orange) color.
 *
 * Note: π and e are *constants*, not operators - tapping one inserts a
 * literal value, the same conceptual action as tapping `7`. They sit in
 * the digit color bucket so the orange in advanced mode is contained to
 * column 4 (which lines up with the basic-mode operator column ×/-/+).
 */
private val OperatorLabels = setOf("+", "-", "×", "÷", "%", "^")

// Width-to-height ratio for every keypad button. 1.6 gives a clean
// "horizontal rectangle" silhouette - wider than tall by ~60%, the
// shape physical desk calculators use. Tweaking this is the single
// knob for "make keys taller / shorter" without touching layout code.
private const val BUTTON_ASPECT_RATIO = 1.6f

// Wider aspect ratio for scientific-function and memory rows: same
// width, smaller height, so those rows are visibly less prominent than
// the digit/operator rows and free up vertical space for the display.
private const val BUTTON_ASPECT_RATIO_COMPACT = 2.6f

// iOS palette tokens, used by both the keypad and the tools sheet so
// the calculator reads as a single visual system. Literal colors rather
// than theme roles because the iOS feel is recognisable specifically by
// these hexes; M3 dynamic-color would dilute it.
private val IosKeyDigitContainer = Color(0xFF505050)
private val IosKeyFunctionContainer = Color(0xFF707070)
private val IosKeyModifierContainer = Color(0xFFA5A5A5)
private val IosKeyOperator = Color(0xFFFF9F0A)
private val IosSheetBackground = Color(0xFF1C1C1E)

// ----- Previews -----

@PreviewLightDark
@Composable
private fun BasicCalculatorPreview() {
    CalculatorTheme {
        Surface {
            BasicCalculatorScreenContent(
                state =
                    BasicCalculatorUiState(
                        expression = "12 × 3 + 4",
                        liveResult = "40",
                    ),
                onEvent = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ScientificCalculatorPreview() {
    CalculatorTheme {
        Surface {
            BasicCalculatorScreenContent(
                state =
                    BasicCalculatorUiState(
                        expression = "sin(30)+log(100)",
                        liveResult = "2.5",
                        scientific = true,
                        angleMode = AngleMode.Degree,
                        memory = BigDecimal("42"),
                    ),
                onEvent = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BasicCalculatorErrorPreview() {
    CalculatorTheme {
        Surface {
            BasicCalculatorScreenContent(
                state =
                    BasicCalculatorUiState(
                        expression = "5 ÷ 0",
                        errorMessage = "Can't divide by zero",
                    ),
                onEvent = {},
            )
        }
    }
}
