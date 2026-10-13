package com.calculator.feature.health.bmi

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.common.input.InputResult
import com.calculator.core.designsystem.component.LifeCalcCard
import com.calculator.core.designsystem.component.LifeCalcNumberField
import com.calculator.core.designsystem.component.LifeCalcOutputRow
import com.calculator.core.designsystem.component.LifeCalcSectionLabel
import com.calculator.core.designsystem.component.LifeCalcSegmented
import com.calculator.core.designsystem.component.LifeCalculatorScaffold
import com.calculator.core.designsystem.component.errorFor
import com.calculator.core.domain.health.BmiCalculator

/**
 * Body-Mass-Index calculator. Accepts metric (cm + kg) or imperial
 * (ft + in + lb) input, calls into [BmiCalculator], and reports the
 * BMI value plus the WHO category label (Underweight/Normal/etc).
 *
 * Defaults are global-average adult values (170 cm / 70 kg ≈ BMI 24.2,
 * which lands in "Normal") so the screen always renders a sensible
 * result the moment it appears.
 *
 * @param onOpenMenu Opens the app's tools menu.
 * @param viewModel Remembers the last height and weight units.
 */
@Composable
fun BmiScreen(
    onOpenMenu: () -> Unit,
    viewModel: BmiViewModel = hiltViewModel(),
) {
    // Read persisted unit preferences so the user lands on whichever
    // unit they used last for each axis. Height and weight have
    // independent toggles - mixing them (feet/inches for height, kg
    // for weight) is common and used to be impossible when both were
    // forced by a single metric/imperial system toggle. That coupling
    // caused a real bug: entering 6'1" 93 kg with the system on
    // Imperial silently interpreted the 93 as pounds and reported
    // "Underweight" (BMI 12 instead of 27).
    val userSettings by viewModel.settings.collectAsStateWithLifecycle()
    var heightUnitIdx by rememberSaveable(userSettings.bmiHeightImperial) {
        mutableIntStateOf(if (userSettings.bmiHeightImperial) 1 else 0)
    }
    var weightUnitIdx by rememberSaveable(userSettings.bmiWeightImperial) {
        mutableIntStateOf(if (userSettings.bmiWeightImperial) 1 else 0)
    }
    // Both unit systems' fields are kept in state simultaneously so
    // toggling units preserves what was last typed in the other unit.
    // Saveable, so typed values survive the app being killed in the
    // background.
    var heightCm by rememberSaveable { mutableStateOf("170") }
    var weightKg by rememberSaveable { mutableStateOf("70") }
    var heightFt by rememberSaveable { mutableStateOf("5") }
    var heightIn by rememberSaveable { mutableStateOf("10") }
    var weightLb by rememberSaveable { mutableStateOf("170") }
    val input =
        remember(heightUnitIdx, weightUnitIdx, heightCm, heightFt, heightIn, weightKg, weightLb) {
            val weightImperial = weightUnitIdx == 1
            bmiInput(
                heightImperial = heightUnitIdx == 1,
                heightCm = heightCm,
                heightFeet = heightFt,
                heightInches = heightIn,
                weightImperial = weightImperial,
                weight = if (weightImperial) weightLb else weightKg,
            )
        }

    LifeCalculatorScaffold(
        title = stringResource(R.string.bmi_title),
        onOpenMenu = onOpenMenu,
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
                    viewModel.setHeightImperial(it == 1)
                },
            )
            if (heightUnitIdx == 0) {
                LifeCalcNumberField(
                    label = stringResource(R.string.bmi_height),
                    value = heightCm,
                    onValueChange = { heightCm = it },
                    suffix = stringResource(R.string.bmi_unit_cm),
                    error = input.errorFor(R.string.bmi_height),
                )
            } else {
                LifeCalcNumberField(
                    label = stringResource(R.string.bmi_height_feet),
                    value = heightFt,
                    onValueChange = { heightFt = it },
                    suffix = stringResource(R.string.bmi_unit_ft),
                    error = input.errorFor(R.string.bmi_height_feet),
                )
                LifeCalcNumberField(
                    label = stringResource(R.string.bmi_height_inches),
                    value = heightIn,
                    onValueChange = { heightIn = it },
                    suffix = stringResource(R.string.bmi_unit_in),
                    error = input.errorFor(R.string.bmi_height_inches),
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
                    viewModel.setWeightImperial(it == 1)
                },
            )
            if (weightUnitIdx == 0) {
                LifeCalcNumberField(
                    label = stringResource(R.string.bmi_weight),
                    value = weightKg,
                    onValueChange = { weightKg = it },
                    suffix = stringResource(R.string.bmi_unit_kg),
                    error = input.errorFor(R.string.bmi_weight),
                )
            } else {
                LifeCalcNumberField(
                    label = stringResource(R.string.bmi_weight),
                    value = weightLb,
                    onValueChange = { weightLb = it },
                    suffix = stringResource(R.string.bmi_unit_lb),
                    error = input.errorFor(R.string.bmi_weight),
                )
            }
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.bmi_section_result))
            when (input) {
                is InputResult.Ok -> {
                    val result = input.value
                    LifeCalcOutputRow(
                        label = stringResource(R.string.bmi_label_bmi),
                        value = NumberFormatter.format(result.bmi, minFractionDigits = 2, maxFractionDigits = 2),
                        accent = true,
                    )
                    LifeCalcOutputRow(
                        label = stringResource(R.string.bmi_category),
                        value = result.category.label,
                    )
                }
                is InputResult.Invalid ->
                    Text(
                        text = stringResource(R.string.bmi_error_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
            }
        }
    }
}
