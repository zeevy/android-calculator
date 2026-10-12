package com.calculator.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.calculator.R
import com.calculator.feature.basic.ui.BasicCalculatorEvent
import com.calculator.feature.floating.rememberFloatingCalculatorLauncher
import com.calculator.feature.history.HistorySheetContent
import com.calculator.feature.settings.SettingsSheetContent
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
 * The app's tools menu: a bottom sheet with the tool grid, History and
 * Settings. The app shell hosts one instance above the NavHost, so tool
 * screens only need an `onOpenMenu` callback.
 *
 * Actions that change the basic calculator (Home, Advanced, reusing a
 * history entry) go straight to its ViewModel, which lives on the basic
 * calculator's back stack entry and keeps its state across process death.
 *
 * @param openSheet Which sub-sheet is visible, or null when closed.
 * @param onOpenSheet Switches the visible sub-sheet.
 * @param onDismiss Hides the menu.
 * @param navController The app's nav controller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsMenuOverlay(
    openSheet: ToolsMenuSheet?,
    onOpenSheet: (ToolsMenuSheet) -> Unit,
    onDismiss: () -> Unit,
    navController: NavController,
) {
    if (openSheet == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }
        onDismiss()
    }
    val calculator = rememberBasicCalculatorViewModel(navController)
    val calculatorState by calculator.state.collectAsStateWithLifecycle()
    val currentEntry by navController.currentBackStackEntryAsState()
    val launchFloating = rememberFloatingCalculatorLauncher()
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) },
    ) {
        when (openSheet) {
            ToolsMenuSheet.Tools ->
                ToolsGrid(
                    current = currentEntry?.destination,
                    scientific = calculatorState.scientific,
                    onOpenCalculator = { scientific ->
                        calculator.onEvent(BasicCalculatorEvent.SetScientific(scientific))
                        close()
                        navController.openTool(BasicCalculatorRoute)
                    },
                    onOpenHistory = { onOpenSheet(ToolsMenuSheet.History) },
                    onOpenSettings = { onOpenSheet(ToolsMenuSheet.Settings) },
                    onNavigate = { route ->
                        close()
                        navController.openTool(route)
                    },
                )
            ToolsMenuSheet.History ->
                HistorySheetContent(
                    onReuseExpression = { expr ->
                        calculator.onEvent(BasicCalculatorEvent.Reuse(expr))
                        close()
                        navController.openTool(BasicCalculatorRoute)
                    },
                    onClose = close,
                )
            ToolsMenuSheet.Settings ->
                SettingsSheetContent(onLaunchFloating = launchFloating)
        }
    }
}

/**
 * Tile grid for the Tools sub-sheet.
 *
 * Home and Advanced open the basic calculator in basic or scientific
 * mode. The tile for the page on screen is marked selected; tapping it
 * just closes the sheet.
 *
 * @param current Destination on screen, used to mark the selected tile.
 * @param scientific Whether the basic calculator is in scientific mode.
 * @param onOpenCalculator Opens the basic calculator in the given mode.
 */
@Composable
@Suppress("LongMethod")
private fun ToolsGrid(
    current: NavDestination?,
    scientific: Boolean,
    onOpenCalculator: (scientific: Boolean) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigate: (Any) -> Unit,
) {
    val onCalculator = current?.hasRoute(BasicCalculatorRoute::class) == true

    fun isCurrent(route: Any) = current?.hasRoute(route::class) == true
    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val tiles = buildList {
            add(
                ToolTile(
                    icon = Icons.Filled.Calculate,
                    label = stringResource(R.string.tool_basic),
                    selected = onCalculator && !scientific,
                    onTap = { onOpenCalculator(false) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Functions,
                    label = stringResource(R.string.tool_advanced),
                    selected = onCalculator && scientific,
                    onTap = { onOpenCalculator(true) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Straighten,
                    label = stringResource(R.string.tool_units),
                    selected = isCurrent(UnitConverterRoute),
                    onTap = { onNavigate(UnitConverterRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.AccountBalance,
                    label = stringResource(R.string.tool_loan),
                    selected = isCurrent(LoanRoute),
                    onTap = { onNavigate(LoanRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Receipt,
                    label = stringResource(R.string.tool_gst),
                    selected = isCurrent(GstRoute),
                    onTap = { onNavigate(GstRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.LocalOffer,
                    label = stringResource(R.string.tool_discount),
                    selected = isCurrent(DiscountRoute),
                    onTap = { onNavigate(DiscountRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Restaurant,
                    label = stringResource(R.string.tool_tipsplit),
                    selected = isCurrent(TipSplitRoute),
                    onTap = { onNavigate(TipSplitRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Savings,
                    label = stringResource(R.string.tool_investment),
                    selected = isCurrent(InvestmentRoute),
                    onTap = { onNavigate(InvestmentRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Percent,
                    label = stringResource(R.string.tool_percent),
                    selected = isCurrent(PercentRoute),
                    onTap = { onNavigate(PercentRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Code,
                    label = stringResource(R.string.tool_basecalc),
                    selected = isCurrent(BaseConverterRoute),
                    onTap = { onNavigate(BaseConverterRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.MonitorWeight,
                    label = stringResource(R.string.tool_bmi),
                    selected = isCurrent(BmiRoute),
                    onTap = { onNavigate(BmiRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Cake,
                    label = stringResource(R.string.tool_age),
                    selected = isCurrent(AgeRoute),
                    onTap = { onNavigate(AgeRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.DateRange,
                    label = stringResource(R.string.tool_date_diff),
                    selected = isCurrent(DateDiffRoute),
                    onTap = { onNavigate(DateDiffRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Public,
                    label = stringResource(R.string.tool_timezone),
                    selected = isCurrent(TimezoneRoute),
                    onTap = { onNavigate(TimezoneRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.Filled.Favorite,
                    label = stringResource(R.string.tool_ovulation),
                    selected = isCurrent(OvulationRoute),
                    onTap = { onNavigate(OvulationRoute) },
                ),
            )
            add(
                ToolTile(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = stringResource(R.string.tool_tape),
                    selected = isCurrent(TapeRoute),
                    onTap = { onNavigate(TapeRoute) },
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
    val containerColor = if (tile.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (tile.selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
