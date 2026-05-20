package com.calculator.feature.health.bmi

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.calculator.navigation.BmiRoute

/**
 * Body-Mass-Index calculator. Accepts metric (cm + kg) or imperial
 * (ft + in + lb) input, calls into [BmiCalculator], and reports the
 * BMI value plus the WHO category label (Underweight/Normal/etc).
 *
 * Defaults are global-average adult values (170 cm / 70 kg ≈ BMI 24.2,
 * which lands in "Normal") so the screen always renders a sensible
 * result the moment it appears.
 *
 * @param onNavigate Jump to another tool / home. Wired to the scaffold's
 *   hamburger menu.
 */
@Composable
fun BmiScreen(onNavigate: (Any) -> Unit) {
    // Read persisted unit preferences so the user lands on whichever
    // unit they used last for each axis. Height and weight have
    // independent toggles - mixing them (feet/inches for height, kg
    // for weight) is common and used to be impossible when both were
    // forced by a single metric/imperial system toggle. That coupling
    // caused a real bug: entering 6'1" 93 kg with the system on
    // Imperial silently interpreted the 93 as pounds and reported
    // "Underweight" (BMI 12 instead of 27).
    val settingsViewModel: com.calculator.feature.settings.SettingsViewModel =
        androidx.hilt.navigation.compose.hiltViewModel()
    val userSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var heightUnitIdx by remember(userSettings.bmiHeightImperial) {
        mutableIntStateOf(if (userSettings.bmiHeightImperial) 1 else 0)
    }
    var weightUnitIdx by remember(userSettings.bmiWeightImperial) {
        mutableIntStateOf(if (userSettings.bmiWeightImperial) 1 else 0)
    }
    // Both unit systems' fields are kept in state simultaneously so
    // toggling units preserves what was last typed in the other unit.
    var heightCm by remember { mutableStateOf("170") }
    var weightKg by remember { mutableStateOf("70") }
    var heightFt by remember { mutableStateOf("5") }
    var heightIn by remember { mutableStateOf("10") }
    var weightLb by remember { mutableStateOf("170") }

    LifeCalculatorScaffold(
        title = stringResource(R.string.bmi_title),
        currentRoute = BmiRoute,
        onNavigate = onNavigate,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.bmi_section_height))
            LifeCalcSegmented(
                options = listOf(
                    stringResource(R.string.bmi_unit_cm),
                    stringResource(R.string.bmi_unit_ft_in),
                ),
                selectedIndex = heightUnitIdx,
                onSelect = {
                    heightUnitIdx = it
                    settingsViewModel.setBmiHeightImperial(it == 1)
                },
            )
            if (heightUnitIdx == 0) {
                LifeCalcNumberField(
                    label = stringResource(R.string.bmi_height),
                    value = heightCm,
                    onValueChange = { heightCm = it },
                    suffix = stringResource(R.string.bmi_unit_cm),
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
            }
        }
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.bmi_section_weight))
            LifeCalcSegmented(
                options = listOf(
                    stringResource(R.string.bmi_unit_kg),
                    stringResource(R.string.bmi_unit_lb),
                ),
                selectedIndex = weightUnitIdx,
                onSelect = {
                    weightUnitIdx = it
                    settingsViewModel.setBmiWeightImperial(it == 1)
                },
            )
            if (weightUnitIdx == 0) {
                LifeCalcNumberField(
                    label = stringResource(R.string.bmi_weight),
                    value = weightKg,
                    onValueChange = { weightKg = it },
                    suffix = stringResource(R.string.bmi_unit_kg),
                )
            } else {
                LifeCalcNumberField(
                    label = stringResource(R.string.bmi_weight),
                    value = weightLb,
                    onValueChange = { weightLb = it },
                    suffix = stringResource(R.string.bmi_unit_lb),
                )
            }
        }

        // Resolve into the canonical metric inputs (cm + kg) the
        // BmiCalculator expects, then delegate. Mixing ft/in height
        // with kg weight just funnels both into the same engine call.
        val result =
            runCatching {
                val heightCmResolved =
                    if (heightUnitIdx == 0) {
                        heightCm.toDouble()
                    } else {
                        val totalInches = heightFt.toInt() * 12 + heightIn.toDouble()
                        totalInches * 2.54
                    }
                val weightKgResolved =
                    if (weightUnitIdx == 0) {
                        weightKg.toDouble()
                    } else {
                        weightLb.toDouble() * 0.45359237
                    }
                BmiCalculator.metric(heightCmResolved, weightKgResolved)
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
