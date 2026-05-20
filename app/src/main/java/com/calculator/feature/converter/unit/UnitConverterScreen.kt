package com.calculator.feature.converter.unit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calculator.R
import com.calculator.core.domain.converter.ConverterUnit
import com.calculator.core.domain.converter.UnitCategory
import com.calculator.feature.lifecalc.PendingExpressionHolder
import com.calculator.feature.lifecalc.ToolsMenuOverlay
import com.calculator.feature.lifecalc.ToolsMenuSheet
import com.calculator.navigation.BasicCalculatorRoute
import com.calculator.navigation.UnitConverterRoute

/**
 * Full-screen unit converter.
 *
 *  - Top: category tabs (Length, Area, ..., Power) in a horizontal scroll.
 *  - Middle: stacked "From" / "To" cards, with swap button between them.
 *  - Each card has a unit selector (opens a bottom-sheet picker) and
 *    either an editable number field ("From") or a read-only value ("To").
 *
 * iOS palette: black canvas, light-grey row backgrounds, operator
 * orange for accents (swap button, selected tab).
 */
@Composable
fun UnitConverterScreen(
    onNavigate: (Any) -> Unit,
    viewModel: UnitConverterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var openSheet by remember { mutableStateOf<ToolsMenuSheet?>(null) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .systemBarsPadding()
                // 12dp matches the basic calculator's outer padding so
                // the top-right hamburger sits at the same pixel.
                .padding(horizontal = 12.dp),
    ) {
        // App-bar row: title on the left, hamburger pinned to the right
        // so it matches the basic calculator's top-right menu position.
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.units_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            IconButton(onClick = { openSheet = ToolsMenuSheet.Tools }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.basic_open_menu),
                    tint = Color.White,
                )
            }
        }

        // Category dropdown: one tap reveals all 11 categories vertically
        // with the current selection marked. Replaced an earlier
        // horizontal-scroll chip row that hid most of the categories
        // off-screen on narrow phones.
        CategoryDropdown(
            categories = UnitCategory.entries,
            selected = state.category,
            onSelect = viewModel::selectCategory,
        )
        Spacer(Modifier.size(24.dp))

        // From / Swap / To stack. Both fields are editable; the
        // ViewModel tracks `lastEdited` and rewrites the OTHER side
        // on every keystroke.
        FromCard(
            unit = state.fromUnit,
            value = state.fromText,
            onValueChange = viewModel::setFromInput,
            onPickUnit = { viewModel.openPicker(UnitConverterUiState.PickerSide.From) },
        )
        Spacer(Modifier.size(12.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = viewModel::swap,
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(ConverterAccent),
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = stringResource(R.string.units_swap),
                    tint = Color.White,
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        ToCard(
            unit = state.toUnit,
            value = state.toText,
            onValueChange = viewModel::setToInput,
            onPickUnit = { viewModel.openPicker(UnitConverterUiState.PickerSide.To) },
        )
    }

    // Unit picker. Opens for whichever side requested it.
    val picker = state.pickerOpen
    if (picker != null) {
        UnitPickerSheet(
            units = state.units,
            currentSymbol =
                when (picker) {
                    UnitConverterUiState.PickerSide.From -> state.fromUnit?.symbol
                    UnitConverterUiState.PickerSide.To -> state.toUnit?.symbol
                },
            onPick = { unit ->
                when (picker) {
                    UnitConverterUiState.PickerSide.From -> viewModel.setFromUnit(unit)
                    UnitConverterUiState.PickerSide.To -> viewModel.setToUnit(unit)
                }
            },
            onDismiss = viewModel::dismissPicker,
        )
    }

    ToolsMenuOverlay(
        openSheet = openSheet,
        onDismiss = { openSheet = null },
        currentRoute = UnitConverterRoute,
        isOnBasicCalc = false,
        scientific = false,
        onToggleScientific = {},
        onOpenHistory = { openSheet = ToolsMenuSheet.History },
        onOpenSettings = { openSheet = ToolsMenuSheet.Settings },
        onNavigate = onNavigate,
        onReuseExpression = { expr ->
            PendingExpressionHolder.expression = expr
            onNavigate(BasicCalculatorRoute)
        },
    )
}

/**
 * Category picker rendered as a single trigger pill plus a modal
 * bottom-sheet list of all 11 categories.
 *
 * Compared to the dropdown variant: the sheet uses the full width of
 * the screen, every option has a comfortable touch target with the
 * selected one highlighted, and the picker reads as a dedicated
 * "choose one" step instead of a contextual menu floating off the
 * trigger.
 *
 * Internal `sheetOpen` state is local; the parent only needs to know
 * when [onSelect] fires.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<UnitCategory>,
    selected: UnitCategory,
    onSelect: (UnitCategory) -> Unit,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Trigger pill: identical shell to the dropdown variant so users
    // who tried both see the same affordance up top.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBackground)
                .clickable { sheetOpen = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = selected.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = Color.White,
        )
    }

    if (sheetOpen) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { sheetOpen = false },
            containerColor = Color.Black,
            // Force-white content so titles render correctly in light
            // system theme too - same fix we applied to the main
            // calculator's bottom sheets.
            contentColor = Color.White,
        ) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = "Category",
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                categories.forEach { category ->
                    val isSelected = category == selected
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(category)
                                    sheetOpen = false
                                }.padding(horizontal = 24.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) ConverterAccent else Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = ConverterAccent,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top "From" card with the editable number field. Custom
 * `decorationBox` paints a faded "0" placeholder when the field is
 * empty - BasicTextField doesn't expose Material's built-in placeholder.
 */
@Composable
private fun FromCard(
    unit: ConverterUnit?,
    value: String,
    onValueChange: (String) -> Unit,
    onPickUnit: () -> Unit,
) {
    UnitCard {
        Column(modifier = Modifier.padding(16.dp)) {
            UnitChooser(unit = unit, onClick = onPickUnit, label = stringResource(R.string.units_from))
            Spacer(Modifier.size(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle =
                    TextStyle(
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                    ),
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush =
                    androidx.compose.ui.graphics
                        .SolidColor(ConverterAccent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = "0",
                                color = Color.White.copy(alpha = 0.3f),
                                style =
                                    MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

/**
 * Symmetric to [FromCard]: same editable text field, same placeholder,
 * same styling. Whichever side the user types into becomes the source
 * for conversion - the ViewModel rewrites the other side on every
 * keystroke. The `decorationBox` paints the faded "0" placeholder
 * because BasicTextField doesn't expose Material's built-in one.
 */
@Composable
private fun ToCard(
    unit: ConverterUnit?,
    value: String,
    onValueChange: (String) -> Unit,
    onPickUnit: () -> Unit,
) {
    UnitCard {
        Column(modifier = Modifier.padding(16.dp)) {
            UnitChooser(unit = unit, onClick = onPickUnit, label = stringResource(R.string.units_to))
            Spacer(Modifier.size(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle =
                    TextStyle(
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                    ),
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush =
                    androidx.compose.ui.graphics
                        .SolidColor(ConverterAccent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = "0",
                                color = Color.White.copy(alpha = 0.3f),
                                style =
                                    MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun UnitCard(content: @Composable () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBackground),
    ) { content() }
}

@Composable
private fun UnitChooser(
    unit: ConverterUnit?,
    onClick: () -> Unit,
    label: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = unit?.let { "${it.displayName} (${it.symbol})" } ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = ConverterAccent,
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun UnitPickerSheet(
    units: List<ConverterUnit>,
    currentSymbol: String?,
    onPick: (ConverterUnit) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = SheetBackground,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(units) { unit ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(unit) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = unit.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color =
                            if (unit.symbol == currentSymbol) {
                                ConverterAccent
                            } else {
                                Color.White
                            },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = unit.symbol,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }
        }
    }
}

private val CardBackground = Color(0xFF2C2C2E)
private val SheetBackground = Color(0xFF1C1C1E)
private val ConverterAccent = Color(0xFFFF9F0A)
