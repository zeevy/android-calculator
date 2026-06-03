package com.calculator.feature.finance.tipsplit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.domain.finance.TipSplitCalculator
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcNumberField
import com.calculator.feature.lifecalc.LifeCalcOutputRow
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import com.calculator.navigation.TipSplitRoute
import java.util.Locale

/**
 * Tip + bill-split calculator.
 *
 * Inputs: bill amount, tip percentage (with quick presets), headcount,
 * and a "round up per person" toggle. Outputs the tip amount, the
 * grand total, and each person's share.
 *
 * Defaults render a populated result on entry (₹1,000 bill / 10% tip /
 * 2 people) so the first frame shows the row layout.
 *
 * @param onNavigate Jump to another tool / home.
 */
@Composable
fun TipSplitScreen(onNavigate: (Any) -> Unit) {
    var bill by remember { mutableStateOf("1000") }
    var tipAmount by remember { mutableStateOf("100") }
    var people by remember { mutableIntStateOf(2) }
    var roundUp by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    val result =
        runCatching {
            TipSplitCalculator.compute(
                bill = bill.toDouble(),
                tipAmount = tipAmount.toDouble(),
                people = people,
                roundUpPerPerson = roundUp,
            )
        }.getOrNull()

    LifeCalculatorScaffold(
        title = stringResource(R.string.tipsplit_title),
        currentRoute = TipSplitRoute,
        onNavigate = onNavigate,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.tipsplit_section_inputs))
            LifeCalcNumberField(
                label = stringResource(R.string.tipsplit_bill),
                value = bill,
                onValueChange = { bill = it },
            )
            LifeCalcNumberField(
                label = stringResource(R.string.tipsplit_tip_amount_input),
                value = tipAmount,
                onValueChange = { tipAmount = it },
            )
            PeopleStepper(
                people = people,
                onChange = { people = it },
            )
            RoundUpRow(checked = roundUp, onCheckedChange = { roundUp = it })
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.tipsplit_section_result))
            if (result == null) {
                Text(
                    text = stringResource(R.string.tipsplit_error_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
                )
            } else {
                LifeCalcOutputRow(
                    label = stringResource(R.string.tipsplit_tip_amount),
                    value = money(result.tip),
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.tipsplit_total),
                    value = money(result.total),
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.tipsplit_per_person),
                    value = money(result.perPerson),
                    accent = true,
                )
            }
        }
    }
}

/** Headcount stepper: `-`  N people  `+`. */
@Composable
private fun PeopleStepper(people: Int, onChange: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.tipsplit_people),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        StepperButton(symbol = "-", enabled = people > 1) {
            onChange((people - 1).coerceAtLeast(1))
        }
        Spacer(Modifier.size(16.dp))
        Text(
            text = people.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = scheme.primary,
        )
        Spacer(Modifier.size(16.dp))
        StepperButton(symbol = "+", enabled = true) {
            onChange(people + 1)
        }
    }
}

@Composable
private fun StepperButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    Text(
        text = symbol,
        style = MaterialTheme.typography.titleLarge,
        color = scheme.onSurface.copy(alpha = alpha),
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(scheme.onSurface.copy(alpha = 0.08f * alpha))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(top = 2.dp),
    )
}

@Composable
private fun RoundUpRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.tipsplit_round_up),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = scheme.onSurface,
                    checkedTrackColor = scheme.primary,
                ),
        )
    }
}

private fun money(value: Double): String =
    NumberFormatter.money(value, Locale.getDefault())

// Visual dim for the disabled stepper button (can't go below 1 person).
private const val DISABLED_ALPHA = 0.3f
