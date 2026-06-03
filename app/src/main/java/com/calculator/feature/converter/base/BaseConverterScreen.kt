package com.calculator.feature.converter.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.R
import com.calculator.core.domain.math.BaseConverter
import com.calculator.feature.lifecalc.LifeCalcCard
import com.calculator.feature.lifecalc.LifeCalcSectionLabel
import com.calculator.feature.lifecalc.LifeCalculatorScaffold
import com.calculator.navigation.BaseConverterRoute

/**
 * Number-base converter (binary / octal / decimal / hex).
 *
 * Four parallel text fields, all editable. Whichever the user typed
 * into last is the source; the other three are recomputed from it on
 * every keystroke. Hex is uppercased on display; underscores and
 * mixed case are tolerated on input (so `1010_1100` and `0xfF` round-
 * trip cleanly).
 *
 * Defaults render the value 42 in all four bases so the layout is
 * populated on first frame.
 *
 * @param onNavigate Jump to another tool / home.
 */
@Composable
fun BaseConverterScreen(onNavigate: (Any) -> Unit) {
    // Seed each field independently so a mid-edit string like "" or
    // "0xff" doesn't have to round-trip through Long before showing
    // back. lastEdited tracks which radix is the source-of-truth and
    // which three should be rewritten on the next recomposition.
    var bin by remember { mutableStateOf("101010") }
    var oct by remember { mutableStateOf("52") }
    var dec by remember { mutableStateOf("42") }
    var hex by remember { mutableStateOf("2A") }
    var lastEdited by remember { mutableStateOf(BaseConverter.DEC) }

    val sourceText =
        when (lastEdited) {
            BaseConverter.BIN -> bin
            BaseConverter.OCT -> oct
            BaseConverter.HEX -> hex
            else -> dec
        }
    val parsed = BaseConverter.parse(sourceText, lastEdited)

    if (parsed != null) {
        // Re-derive the other three sides. Each branch only rewrites
        // the non-source fields so the user's in-flight string in the
        // source field is preserved exactly as typed.
        if (lastEdited != BaseConverter.BIN) bin = BaseConverter.format(parsed, BaseConverter.BIN)
        if (lastEdited != BaseConverter.OCT) oct = BaseConverter.format(parsed, BaseConverter.OCT)
        if (lastEdited != BaseConverter.DEC) dec = BaseConverter.format(parsed, BaseConverter.DEC)
        if (lastEdited != BaseConverter.HEX) hex = BaseConverter.format(parsed, BaseConverter.HEX)
    }

    LifeCalculatorScaffold(
        title = stringResource(R.string.basecalc_title),
        currentRoute = BaseConverterRoute,
        onNavigate = onNavigate,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.basecalc_section_values))
            BaseField(
                label = stringResource(R.string.basecalc_dec),
                value = dec,
                onValueChange = {
                    // Decimal: digit-only, accept negative not relevant
                    // (unsigned semantics).
                    val filtered = it.filter { ch -> ch.isDigit() || ch == '_' }
                    dec = filtered
                    lastEdited = BaseConverter.DEC
                },
                keyboard = KeyboardType.Number,
            )
            BaseField(
                label = stringResource(R.string.basecalc_hex),
                value = hex,
                onValueChange = {
                    val filtered = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }
                    hex = filtered.uppercase()
                    lastEdited = BaseConverter.HEX
                },
                keyboard = KeyboardType.Text,
                monospace = true,
            )
            BaseField(
                label = stringResource(R.string.basecalc_oct),
                value = oct,
                onValueChange = {
                    val filtered = it.filter { ch -> (ch in '0'..'7') || ch == '_' }
                    oct = filtered
                    lastEdited = BaseConverter.OCT
                },
                keyboard = KeyboardType.Number,
                monospace = true,
            )
            BaseField(
                label = stringResource(R.string.basecalc_bin),
                value = bin,
                onValueChange = {
                    val filtered = it.filter { ch -> ch == '0' || ch == '1' || ch == '_' }
                    bin = filtered
                    lastEdited = BaseConverter.BIN
                },
                keyboard = KeyboardType.Number,
                monospace = true,
            )
            if (parsed == null) {
                Text(
                    text = stringResource(R.string.basecalc_error_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Label-above-value field with a monospace option for the bases where
 * column alignment helps the eye (bin/oct/hex). Decimal stays in the
 * normal weight so it doesn't read like code.
 */
@Composable
private fun BaseField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboard: KeyboardType,
    monospace: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle =
                TextStyle(
                    color = scheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily =
                        if (monospace) androidx.compose.ui.text.font.FontFamily.Monospace else null,
                ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            cursorBrush = SolidColor(scheme.primary),
        )
    }
}
