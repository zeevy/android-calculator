package com.calculator.feature.health.bmi

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.calculator.R
import com.calculator.core.domain.health.BmiCalculator
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcNumberField
import com.calculator.feature.lifecalc.LifeCalcOutputRow
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalcSegmented
import com.calculator.feature.lifecalc.LifeCalculatorScaffold

/**
 * Body-Mass-Index calculator. Accepts metric (cm + kg) or imperial
 * (ft + in + lb) input, calls into [BmiCalculator], and reports the
 * BMI value plus the WHO category label (Underweight/Normal/etc).
 *
 * Defaults are global-average adult values (170 cm / 70 kg ≈ BMI 24.2,
 * which lands in "Normal") so the screen always renders a sensible
 * result the moment it appears.
 *
 * @param onUp Pop the calculator from the back stack.
 */
@Composable
fun BmiScreen(onUp: () -> Unit) {
    var unitIdx by remember { mutableIntStateOf(0) } // 0=Metric, 1=Imperial
    // Both unit systems' fields are kept in state simultaneously so
    // toggling units preserves what was last typed in the other system
    // - common pattern for height/weight where the user might want to
    // compare "what's 170 cm in feet/inches?".
    // Metric inputs
    var heightCm by remember { mutableStateOf("170") }
    var weightKg by remember { mutableStateOf("70") }
    // Imperial inputs
    var heightFt by remember { mutableStateOf("5") }
    var heightIn by remember { mutableStateOf("10") }
    var weightLb by remember { mutableStateOf("170") }

    LifeCalculatorScaffold(title = stringResource(R.string.bmi_title), onUp = onUp) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.bmi_section_units))
            LifeCalcSegmented(
                options = listOf(
                    stringResource(R.string.bmi_units_metric),
                    stringResource(R.string.bmi_units_imperial),
                ),
                selectedIndex = unitIdx,
                onSelect = { unitIdx = it },
            )
            if (unitIdx == 0) {
                LifeCalcNumberField(
                    label = stringResource(R.string.bmi_height),
                    value = heightCm,
                    onValueChange = { heightCm = it },
                    suffix = stringResource(R.string.bmi_unit_cm),
                )
                LifeCalcNumberField(
                    label = stringResource(R.string.bmi_weight),
                    value = weightKg,
                    onValueChange = { weightKg = it },
                    suffix = stringResource(R.string.bmi_unit_kg),
                )
            } else {
                LifeCalcNumberField(
                    label = stringResource(R.string.bmi_height_feet),
                    value = heightFt,
                    onValueChange = { heightFt = it },
                    suffix = stringResource(R.string.bmi_unit_ft),
                )
                LifeCalcNumberField(
                    label = stringResource(R.string.bmi_height_inches),
                    value = heightIn,
                    onValueChange = { heightIn = it },
                    suffix = stringResource(R.string.bmi_unit_in),
                )
                LifeCalcNumberField(
                    label = stringResource(R.string.bmi_weight),
                    value = weightLb,
                    onValueChange = { weightLb = it },
                    suffix = stringResource(R.string.bmi_unit_lb),
                )
            }
        }

        val result =
            runCatching {
                if (unitIdx == 0) {
                    BmiCalculator.metric(heightCm.toDouble(), weightKg.toDouble())
                } else {
                    BmiCalculator.imperial(
                        heightFeet = heightFt.toInt(),
                        heightInches = heightIn.toDouble(),
                        weightLb = weightLb.toDouble(),
                    )
                }
            }.getOrNull()

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.bmi_section_result))
            if (result == null) {
                Text(
                    text = stringResource(R.string.bmi_error_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            } else {
                LifeCalcOutputRow(
                    label = stringResource(R.string.bmi_label_bmi),
                    value = "%.2f".format(result.bmi),
                    accent = true,
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.bmi_category),
                    value = result.category.label,
                )
            }
        }
    }
}
