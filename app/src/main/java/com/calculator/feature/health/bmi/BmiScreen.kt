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
import com.calculator.core.domain.health.BmiCalculator
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcNumberField
import com.calculator.feature.lifecalc.LifeCalcOutputRow
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalcSegmented
import com.calculator.feature.lifecalc.LifeCalculatorScaffold

@Composable
fun BmiScreen(onUp: () -> Unit) {
    var unitIdx by remember { mutableIntStateOf(0) } // 0=Metric, 1=Imperial
    // Metric inputs
    var heightCm by remember { mutableStateOf("170") }
    var weightKg by remember { mutableStateOf("70") }
    // Imperial inputs
    var heightFt by remember { mutableStateOf("5") }
    var heightIn by remember { mutableStateOf("10") }
    var weightLb by remember { mutableStateOf("170") }

    LifeCalculatorScaffold(title = "BMI", onUp = onUp) {
        LifeCalcCard {
            LifeCalcSectionLabel("Units")
            LifeCalcSegmented(
                options = listOf("Metric (cm/kg)", "Imperial (ft·in/lb)"),
                selectedIndex = unitIdx,
                onSelect = { unitIdx = it },
            )
            if (unitIdx == 0) {
                LifeCalcNumberField("Height", heightCm, { heightCm = it }, suffix = "cm")
                LifeCalcNumberField("Weight", weightKg, { weightKg = it }, suffix = "kg")
            } else {
                LifeCalcNumberField("Height (feet)", heightFt, { heightFt = it }, suffix = "ft")
                LifeCalcNumberField("Height (inches)", heightIn, { heightIn = it }, suffix = "in")
                LifeCalcNumberField("Weight", weightLb, { weightLb = it }, suffix = "lb")
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
            LifeCalcSectionLabel("Result")
            if (result == null) {
                Text(
                    text = "Enter your height and weight.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            } else {
                LifeCalcOutputRow("BMI", "%.2f".format(result.bmi), accent = true)
                LifeCalcOutputRow("Category", result.category.label)
            }
        }
    }
}
