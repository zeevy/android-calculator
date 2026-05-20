package com.calculator.feature.lifecalc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.calculator.R
import com.calculator.feature.history.HistorySheetContent
import com.calculator.feature.settings.SettingsSheetContent
import com.calculator.navigation.AgeRoute
import com.calculator.navigation.BasicCalculatorRoute
import com.calculator.navigation.BmiRoute
import com.calculator.navigation.DateDiffRoute
import com.calculator.navigation.DiscountRoute
import com.calculator.navigation.GstRoute
import com.calculator.navigation.InvestmentRoute
import com.calculator.navigation.LoanRoute
import com.calculator.navigation.OvulationRoute
import com.calculator.navigation.PercentRoute
import com.calculator.navigation.TimezoneRoute
import com.calculator.navigation.TipSplitRoute
import com.calculator.navigation.UnitConverterRoute
import kotlinx.coroutines.launch

/**
 * Which sub-sheet the menu host should display.
 *
 * The hamburger first opens [Tools] (the tile grid); from there the user
 * can jump to [History] or [Settings], which are full takeovers of the
 * same modal sheet rather than separate dialogs.
 */
enum class ToolsMenuSheet { Tools, History, Settings }

/**
 * One-shot expression handoff between tool pages and the basic calculator.
 *
 * The History sheet can be opened from any screen. When the user taps a
 * row from a tool page, the expression is stashed here and navigation
 * jumps to the basic calculator, whose ViewModel consumes the holder on
 * init and clears it. A simple `object` is enough - the handoff is in
 * the same process and the value is read exactly once.
 */
object PendingExpressionHolder {
    @Volatile
    var expression: String? = null

    /**
     * Desired scientific-mode setting for the next basic-calc appearance.
     * Used by the Advanced tile on tool pages: tapping it sets this to
     * `true` and navigates home; the basic calc reads & clears the hint
     * on init. Null = no change requested.
     */
    @Volatile
    var scientificMode: Boolean? = null
}

/**
 * Shared bottom-sheet host for the hamburger menu used across the app.
 *
 * Each screen that needs the menu manages its own [openSheet] state and
 * passes it in; this composable renders nothing when [openSheet] is null
 * and otherwise shows the appropriate sub-sheet on top of the host.
 *
 * @param openSheet Which sub-sheet is currently visible, or null when the
 *   menu is closed.
 * @param onDismiss Invoked when the sheet should be hidden (drag-down,
 *   scrim tap, or after a tile is chosen).
 * @param currentRoute The route this overlay is hosted on; used to
 *   highlight (or no-op) the matching tile.
 * @param isOnBasicCalc True when the host is the basic calculator. Only
 *   then does the Advanced tile show (it toggles scientific mode, which
 *   is meaningless from a tool page).
 * @param scientific Whether the basic calculator is currently in
 *   scientific mode. Drives Basic/Advanced tile selection.
 * @param onToggleScientific Called when Advanced is tapped on basic calc.
 * @param onOpenHistory Asks the host to swap the visible sheet to History.
 * @param onOpenSettings Asks the host to swap the visible sheet to Settings.
 * @param onNavigate Invoked with a destination route when a tile other
 *   than History/Settings is tapped. The host translates this to a real
 *   nav action; see [com.calculator.navigation.CalculatorNavHost].
 * @param onReuseExpression Invoked with the chosen history expression.
 *   On basic calc, this applies the expression directly; on tool pages,
 *   the caller stashes it in [PendingExpressionHolder] and navigates
 *   back to the basic calculator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun ToolsMenuOverlay(
    openSheet: ToolsMenuSheet?,
    onDismiss: () -> Unit,
    currentRoute: Any,
    isOnBasicCalc: Boolean,
    scientific: Boolean,
    onToggleScientific: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigate: (Any) -> Unit,
    onReuseExpression: (String) -> Unit,
) {
    if (openSheet == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }
        onDismiss()
    }
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = IosSheetBackground,
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = IosKeyModifierContainer) },
    ) {
        when (openSheet) {
            ToolsMenuSheet.Tools ->
                ToolsGrid(
                    currentRoute = currentRoute,
                    isOnBasicCalc = isOnBasicCalc,
                    scientific = scientific,
                    onToggleScientific = {
                        onToggleScientific()
                        close()
                    },
                    onOpenHistory = onOpenHistory,
                    onOpenSettings = onOpenSettings,
                    onNavigate = { route ->
                        close()
                        onNavigate(route)
                    },
                )
            ToolsMenuSheet.History ->
                HistorySheetContent(
                    onReuseExpression = { expr ->
                        onReuseExpression(expr)
                        close()
                    },
                    onClose = close,
                )
            ToolsMenuSheet.Settings ->
                SettingsSheetContent(onClose = close)
        }
    }
}

/**
 * Tile grid for the Tools sub-sheet.
 *
 * Home is always first. Advanced appears only on the basic calculator
 * page (its action is "toggle scientific mode", which doesn't apply
 * outside that screen). Tiles for the currently-displayed page are
 * marked selected; tapping the current tile is a no-op (it just closes
 * the sheet via the surrounding overlay).
 */
@Composable
@Suppress("LongParameterList")
private fun ToolsGrid(
    currentRoute: Any,
    isOnBasicCalc: Boolean,
    scientific: Boolean,
    onToggleScientific: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigate: (Any) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        val tiles = buildList {
            // Home (basic calculator). Selected when we're on basic-calc
            // in non-scientific mode; tapping it from a tool page goes
            // back to the basic calculator.
            add(
                ToolTile(
                    icon = Icons.Filled.Calculate,
                    label = stringResource(R.string.tool_basic),
                    selected = isOnBasicCalc && !scientific,
                    onTap = {
                        if (isOnBasicCalc) {
                            if (scientific) onToggleScientific()
                            onNavigate(BasicCalculatorRoute)
                        } else {
                            // Stash desired mode for the basic calc to
                            // pick up on init, then navigate home.
                            PendingExpressionHolder.scientificMode = false
                            onNavigate(BasicCalculatorRoute)
                        }
                    },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Functions,
                    label = stringResource(R.string.tool_advanced),
                    selected = isOnBasicCalc && scientific,
                    onTap = {
                        if (isOnBasicCalc) {
                            if (!scientific) onToggleScientific()
                            onNavigate(BasicCalculatorRoute)
                        } else {
                            PendingExpressionHolder.scientificMode = true
                            onNavigate(BasicCalculatorRoute)
                        }
                    },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Straighten,
                    label = stringResource(R.string.tool_units),
                    selected = currentRoute === UnitConverterRoute,
                    onTap = { onNavigate(UnitConverterRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.AccountBalance,
                    label = stringResource(R.string.tool_loan),
                    selected = currentRoute === LoanRoute,
                    onTap = { onNavigate(LoanRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Receipt,
                    label = stringResource(R.string.tool_gst),
                    selected = currentRoute === GstRoute,
                    onTap = { onNavigate(GstRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.LocalOffer,
                    label = stringResource(R.string.tool_discount),
                    selected = currentRoute === DiscountRoute,
                    onTap = { onNavigate(DiscountRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Restaurant,
                    label = stringResource(R.string.tool_tipsplit),
                    selected = currentRoute === TipSplitRoute,
                    onTap = { onNavigate(TipSplitRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Savings,
                    label = stringResource(R.string.tool_investment),
                    selected = currentRoute === InvestmentRoute,
                    onTap = { onNavigate(InvestmentRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Percent,
                    label = stringResource(R.string.tool_percent),
                    selected = currentRoute === PercentRoute,
                    onTap = { onNavigate(PercentRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.MonitorWeight,
                    label = stringResource(R.string.tool_bmi),
                    selected = currentRoute === BmiRoute,
                    onTap = { onNavigate(BmiRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Cake,
                    label = stringResource(R.string.tool_age),
                    selected = currentRoute === AgeRoute,
                    onTap = { onNavigate(AgeRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.DateRange,
                    label = stringResource(R.string.tool_date_diff),
                    selected = currentRoute === DateDiffRoute,
                    onTap = { onNavigate(DateDiffRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Public,
                    label = stringResource(R.string.tool_timezone),
                    selected = currentRoute === TimezoneRoute,
                    onTap = { onNavigate(TimezoneRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Favorite,
                    label = stringResource(R.string.tool_ovulation),
                    selected = currentRoute === OvulationRoute,
                    onTap = { onNavigate(OvulationRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.History,
                    label = stringResource(R.string.tool_history),
                    selected = false,
                    onTap = onOpenHistory,
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Settings,
                    label = stringResource(R.string.tool_settings),
                    selected = false,
                    onTap = onOpenSettings,
                ),
            )
        }

        tiles.chunked(TOOL_TILE_COLUMNS).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowTiles.forEach { tile ->
                    ToolTileButton(tile = tile, modifier = Modifier.weight(1f))
                }
                repeat(TOOL_TILE_COLUMNS - rowTiles.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.size(8.dp))
        }
        Spacer(Modifier.size(16.dp))
    }
}

private data class ToolTile(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onTap: () -> Unit,
)

@Composable
private fun ToolTileButton(tile: ToolTile, modifier: Modifier = Modifier) {
    val containerColor = if (tile.selected) IosKeyOperator else IosKeyDigitContainer
    val contentColor = Color.White
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(20.dp))
                .background(containerColor)
                .clickable(onClick = tile.onTap)
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

// iOS palette tokens shared with the basic calculator keypad. Public
// (well, internal) so the hamburger overlay and any future surfaces can
// render against the same colors regardless of the system M3 theme.
internal val IosKeyDigitContainer = Color(0xFF505050)
internal val IosKeyModifierContainer = Color(0xFFA5A5A5)
internal val IosKeyOperator = Color(0xFFFF9F0A)
internal val IosSheetBackground = Color(0xFF1C1C1E)
