package com.calculator.feature.finance.gst

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.domain.finance.GstCalculator
import com.calculator.feature.lifecalc.LifeCalcAccent
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcNumberField
import com.calculator.feature.lifecalc.LifeCalcOutputRow
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalcSegmentBackground
import com.calculator.feature.lifecalc.LifeCalcSegmented
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import com.calculator.navigation.GstRoute
import java.util.Locale

/**
 * Indian GST calculator. Splits CGST/SGST/IGST based on intra- vs
 * inter-state, and supports both directions (net→gross and reverse).
 *
 * Phase 8 will gate visibility to `en-IN` locale; for now the screen is
 * reachable from the tools menu unconditionally.
 *
 * Tax split rules:
 *  - **Intra-state** (within one state): the total GST is split 50/50
 *    between CGST (collected by the Centre) and SGST (collected by the
 *    State). IGST is zero.
 *  - **Inter-state** (between two states): the entire total goes to
 *    IGST (collected by the Centre, settled later). CGST and SGST are
 *    zero.
 *
 * @param onNavigate Jump to another tool / home. Wired to the scaffold's
 *   hamburger menu.
 */
@Composable
fun GstScreen(onNavigate: (Any) -> Unit) {
    // Bidirectional inputs: both net and gross are editable. Whichever
    // the user typed into last is the source; the OTHER is recomputed
    // on every keystroke. Replaces the old forward/reverse mode toggle -
    // the toggle was a friction step the user had to think about, and
    // the bidirectional model collapses that decision into the data.
    // Read persisted preferences (intra/inter, rate) so the user lands
    // on whatever they last picked instead of the hard-coded default.
    // collectAsState is fine here - GstScreen is small and the
    // initial-recomposition default of "18% / intra" matches the
    // first-launch fallback anyway, so no visible flash.
    val settingsViewModel: com.calculator.feature.settings.SettingsViewModel =
        androidx.hilt.navigation.compose
            .hiltViewModel()
    val userSettings by settingsViewModel.settings.collectAsStateWithLifecycle()

    var netText by remember { mutableStateOf("1000") }
    var grossText by remember { mutableStateOf("1180") }
    // 0 = user is editing the net field, 1 = user is editing the gross
    // field. Kept as an Int (not a sealed type) because it pairs with a
    // segmented-control style across the life-calc surface; the encoded
    // semantics live next to the call site.
    var lastEdited by remember { mutableIntStateOf(0) }
    // Seed intra/inter and rate from saved preferences; update both
    // local state and persisted settings on every change so the screen
    // remembers across launches.
    var intraStateIdx by remember(userSettings.gstIntraState) {
        mutableIntStateOf(if (userSettings.gstIntraState) 0 else 1)
    }
    var ratePercent by remember(userSettings.gstRate) { mutableStateOf(userSettings.gstRate) }

    // Recompute the non-source side from the source side. Done as a
    // simple derivation rather than inside an effect because the two
    // text fields are local state - we can just compute fresh each
    // recomposition based on lastEdited.
    val rate = ratePercent.toDoubleOrNull()
    val intra = intraStateIdx == 0
    val result: com.calculator.core.domain.finance.GstResult? =
        runCatching {
            requireNotNull(rate)
            if (lastEdited == 0) {
                val net = netText.toDouble()
                GstCalculator.forward(net, rate, intra)
            } else {
                val gross = grossText.toDouble()
                GstCalculator.reverse(gross, rate, intra)
            }
        }.getOrNull()
    // Mirror the recomputed side into the OTHER text field so its
    // BasicTextField shows the formatted value (until the user types
    // into it, at which point lastEdited flips and the roles swap).
    val derivedNetText = result?.net?.let { plain(it) } ?: netText
    val derivedGrossText = result?.gross?.let { plain(it) } ?: grossText
    val netDisplay = if (lastEdited == 0) netText else derivedNetText
    val grossDisplay = if (lastEdited == 1) grossText else derivedGrossText

    LifeCalculatorScaffold(
        title = stringResource(R.string.gst_title),
        currentRoute = GstRoute,
        onNavigate = onNavigate,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.gst_section_direction))
            LifeCalcSegmented(
                options = listOf(
                    stringResource(R.string.gst_intra_state),
                    stringResource(R.string.gst_inter_state),
                ),
                selectedIndex = intraStateIdx,
                onSelect = {
                    intraStateIdx = it
                    settingsViewModel.setGstIntraState(it == 0)
                },
            )
            LifeCalcNumberField(
                label = stringResource(R.string.gst_amount_net),
                value = netDisplay,
                onValueChange = {
                    netText = it
                    lastEdited = 0
                },
            )
            LifeCalcNumberField(
                label = stringResource(R.string.gst_amount_gross),
                value = grossDisplay,
                onValueChange = {
                    grossText = it
                    lastEdited = 1
                },
            )
            RatePresets(
                selected = ratePercent,
                onSelect = {
                    ratePercent = it
                    settingsViewModel.setGstRate(it)
                },
            )
            LifeCalcNumberField(
                label = stringResource(R.string.gst_rate),
                value = ratePercent,
                onValueChange = {
                    ratePercent = it
                    settingsViewModel.setGstRate(it)
                },
                suffix = stringResource(R.string.gst_rate_suffix),
            )
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.gst_section_result))
            if (result == null) {
                Text(
                    text = stringResource(R.string.gst_error_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            } else {
                LifeCalcOutputRow(stringResource(R.string.gst_label_net), money(result.net))
                if (intraStateIdx == 0) {
                    LifeCalcOutputRow(stringResource(R.string.gst_label_cgst), money(result.cgst))
                    LifeCalcOutputRow(stringResource(R.string.gst_label_sgst), money(result.sgst))
                } else {
                    LifeCalcOutputRow(stringResource(R.string.gst_label_igst), money(result.igst))
                }
                LifeCalcOutputRow(
                    label = stringResource(R.string.gst_label_gross),
                    value = money(result.gross),
                    accent = true,
                )
            }
        }
    }
}

// Lightweight plain-number formatter for the editable fields - we don't
// want grouping separators in the BasicTextField (they trip the
// permissive toDouble parse on the next keystroke). The pretty
// currency-formatted display lives in the result card below.
private fun plain(value: Double): String {
    val rounded = kotlin.math.round(value * 100) / 100
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        // %.2f keeps the same number of decimals across locales; the
        // engine accepts both "." and "," via the permissive parse so
        // we don't need a locale-aware separator here.
        String.format(Locale.US, "%.2f", rounded)
    }
}

/**
 * Quick-select row for the standard GST slabs.
 *
 * The four currently-active GST rates in India are 5/12/18/28 percent
 * (the 0% category is exempt goods and isn't useful for a calculator).
 * Tapping a chip writes its string value back into the rate field so
 * the rate input and the chip stay in sync.
 *
 * @param selected Currently chosen rate as a percent string ("18" not "18%").
 * @param onSelect Invoked with the freshly-tapped rate string.
 */
@Composable
private fun RatePresets(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("5", "12", "18", "28").forEach { rate ->
            val isSelected = rate == selected
            Text(
                text = "$rate%",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.Black else Color.White,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) LifeCalcAccent else LifeCalcSegmentBackground)
                        .clickable { onSelect(rate) }
                        .padding(vertical = 10.dp),
            )
        }
    }
}

// Money formatter routes through [NumberFormatter.money] so the
// running locale picks the right grouping (en-IN lakh, de-DE swapped
// separators, etc.).
private fun money(value: Double): String =
    NumberFormatter.money(value, Locale.getDefault())
