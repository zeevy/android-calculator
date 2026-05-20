package com.calculator.feature.smartpaste

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.R
import com.calculator.core.common.format.NumberFormatter
import com.calculator.core.domain.math.SmartPaste
import com.calculator.feature.lifecalc.LifeCalcAccent
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcOutputRow
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import com.calculator.navigation.SmartPasteRoute
import java.util.Locale

/**
 * Smart Paste: drop a block of text in, get back every number plus a
 * summary (count, sum, mean, min, max).
 *
 * The big text field accepts arbitrary input - chat messages, receipts,
 * a CSV column. As the user types or pastes, [SmartPaste.extractNumbers]
 * parses tokens (permissive about grouping commas, leading minus,
 * decimals) and the result card updates live.
 *
 * "Paste" button shortcuts the common case of pulling from the system
 * clipboard. The extracted-numbers list is shown so the user can verify
 * the parse before trusting the summary.
 *
 * @param onNavigate Jump to another tool / home.
 */
@Composable
fun SmartPasteScreen(onNavigate: (Any) -> Unit) {
    var text by remember {
        mutableStateOf(
            // Demo seed so the screen lights up on first open.
            "Apples ₹120\nBread ₹45.50\nMilk ₹60\nEggs ₹85",
        )
    }
    val clipboard = LocalClipboardManager.current

    val numbers = remember(text) { SmartPaste.extractNumbers(text) }
    val summary = remember(numbers) { SmartPaste.aggregate(numbers) }

    LifeCalculatorScaffold(
        title = stringResource(R.string.smartpaste_title),
        currentRoute = SmartPasteRoute,
        onNavigate = onNavigate,
    ) {
        LifeCalcCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                LifeCalcSectionLabel(stringResource(R.string.smartpaste_section_input))
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.smartpaste_action_paste),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LifeCalcAccent,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val clip = clipboard.getText()?.text.orEmpty()
                                if (clip.isNotBlank()) text = clip
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = stringResource(R.string.smartpaste_action_clear),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { text = "" }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle =
                    TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                cursorBrush = SolidColor(LifeCalcAccent),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
            )
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.smartpaste_section_numbers))
            if (numbers.isEmpty()) {
                Text(
                    text = stringResource(R.string.smartpaste_no_numbers),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            } else {
                Text(
                    text = numbers.joinToString(", ") { plain(it) },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Start,
                )
            }
        }

        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.smartpaste_section_summary))
            val s = summary
            if (s == null) {
                Text(
                    text = stringResource(R.string.smartpaste_no_numbers),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            } else {
                LifeCalcOutputRow(
                    label = stringResource(R.string.smartpaste_count),
                    value = s.count.toString(),
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.smartpaste_sum),
                    value = plain(s.sum),
                    accent = true,
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.smartpaste_mean),
                    value = plain(s.mean),
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.smartpaste_min),
                    value = plain(s.min),
                )
                LifeCalcOutputRow(
                    label = stringResource(R.string.smartpaste_max),
                    value = plain(s.max),
                )
            }
        }

    }
}

private fun plain(value: Double): String =
    NumberFormatter.format(
        value = value,
        locale = Locale.getDefault(),
        minFractionDigits = 0,
        maxFractionDigits = 4,
    )
