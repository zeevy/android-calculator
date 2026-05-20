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
 * @param onUp Pop the calculator from the back stack. Wired to the
 *   scaffold's back affordance.
 */
@Composable
fun GstScreen(onUp: () -> Unit) {
    // Direction and intra/inter are stored as int indices (rather than
    // booleans) because the segmented control's onSelect handler hands
    // back the chosen index. Encoding the meaning here keeps the
    // segmented component generic and reusable across the life-calc
    // screens.
    var direction by remember { mutableIntStateOf(0) } // 0=forward, 1=reverse
    var intraStateIdx by remember { mutableIntStateOf(0) } // 0=intra, 1=inter
    // 18% is the most-used standard slab (services, electronics);
    // ₹1,000 lands the user on a result that's easy to sanity-check
    // (18% of 1000 = 180, gross = 1180).
    var amount by remember { mutableStateOf("1000") }
    var ratePercent by remember { mutableStateOf("18") }

    LifeCalculatorScaffold(title = stringResource(R.string.gst_title), onUp = onUp) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.gst_section_direction))
            LifeCalcSegmented(
                options = listOf(
                    stringResource(R.string.gst_direction_add),
                    stringResource(R.string.gst_direction_remove),
                ),
                selectedIndex = direction,
                onSelect = { direction = it },
            )
            LifeCalcSegmented(
                options = listOf(
                    stringResource(R.string.gst_intra_state),
                    stringResource(R.string.gst_inter_state),
                ),
                selectedIndex = intraStateIdx,
                onSelect = { intraStateIdx = it },
            )
            LifeCalcNumberField(
                label = if (direction == 0) {
                    stringResource(R.string.gst_amount_net)
                } else {
                    stringResource(R.string.gst_amount_gross)
                },
                value = amount,
                onValueChange = { amount = it },
            )
            RatePresets(
                selected = ratePercent,
                onSelect = { ratePercent = it },
            )
            LifeCalcNumberField(
                label = stringResource(R.string.gst_rate),
                value = ratePercent,
                onValueChange = { ratePercent = it },
                suffix = stringResource(R.string.gst_rate_suffix),
            )
        }

        val result =
            runCatching {
                val rate = ratePercent.toDouble()
                val intra = intraStateIdx == 0
                val amt = amount.toDouble()
                if (direction == 0) {
                    GstCalculator.forward(amt, rate, intra)
                } else {
                    GstCalculator.reverse(amt, rate, intra)
                }
            }.getOrNull()

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
