package com.calculator.feature.lifecalc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable shell for every life-calculator screen.
 *
 * The seven calculators share the same visual chrome - dark canvas,
 * back arrow + screen title, a column of inputs, a card with outputs -
 * so we lift it into one composable and let each feature plug in only
 * what differs.
 *
 * Each feature module then composes:
 *   LifeCalculatorScaffold(title = "BMI", onUp = onUp) {
 *       InputCard { ... }
 *       OutputCard { ... }
 *   }
 */
@Composable
fun LifeCalculatorScaffold(
    title: String,
    onUp: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(LifeCalcBackground)
                .systemBarsPadding()
                .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onUp) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
            Spacer(Modifier.size(24.dp))
        }
    }
}

/** A bordered "card" container; use for grouping inputs or outputs. */
@Composable
fun LifeCalcCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LifeCalcCardBackground)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

/** Inline section title rendered inside a card. */
@Composable
fun LifeCalcSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.55f),
    )
}

/**
 * Labelled number field with the same iOS-flavoured look as the rest
 * of the app: dim white label, white bold value, decimal IME.
 */
@Composable
fun LifeCalcNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String? = null,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
        )
        Spacer(Modifier.size(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle =
                    TextStyle(
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(LifeCalcAccent),
                modifier = Modifier.weight(1f),
            )
            if (suffix != null) {
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** "Label .... value" output line; used in result cards. */
@Composable
fun LifeCalcOutputRow(label: String, value: String, accent: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = if (accent) FontWeight.Bold else FontWeight.SemiBold,
                ),
            color = if (accent) LifeCalcAccent else Color.White,
            textAlign = TextAlign.End,
        )
    }
}

/**
 * Two-option segmented control. Use for toggles like Metric/Imperial,
 * Forward/Reverse, Intra-state/Inter-state.
 */
@Composable
fun LifeCalcSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LifeCalcSegmentBackground)
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { idx, opt ->
            val selected = idx == selectedIndex
            Text(
                text = opt,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) Color.Black else Color.White,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) LifeCalcAccent else Color.Transparent)
                        .clickable { onSelect(idx) }
                        .padding(vertical = 10.dp),
            )
        }
    }
}

internal val LifeCalcAccent = Color(0xFFFF9F0A)
internal val LifeCalcBackground = Color.Black
internal val LifeCalcCardBackground = Color(0xFF1C1C1E)
internal val LifeCalcSegmentBackground = Color(0xFF2C2C2E)

/** Suppress "unused" warnings if a calculator screen doesn't use LazyColumn. */
@Suppress("unused")
private val _lazyColumnUsage: @Composable () -> Unit = { LazyColumn(modifier = Modifier) {} }

/** Compose helper for Box import resolution (we use Box in some output cards). */
@Suppress("unused")
private val _boxUsage: @Composable () -> Unit = { Box(modifier = Modifier) {} }
