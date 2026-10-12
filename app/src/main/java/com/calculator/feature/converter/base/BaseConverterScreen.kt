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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.calculator.core.designsystem.component.LifeCalcCard
import com.calculator.core.designsystem.component.LifeCalcSectionLabel
import com.calculator.core.designsystem.component.LifeCalculatorScaffold
import com.calculator.core.domain.math.BaseConverter

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
 * @param onOpenMenu Opens the app's tools menu.
 */
@Composable
fun BaseConverterScreen(onOpenMenu: () -> Unit) {
    // Only the field the user is typing in keeps its raw text, so a
    // mid-edit string like "" or "1_0" shows back exactly as typed. The
    // other three are formatted from the last value that parsed.
    var sourceText by remember { mutableStateOf("42") }
    var lastEdited by remember { mutableIntStateOf(BaseConverter.DEC) }
    var value by remember { mutableLongStateOf(DEFAULT_VALUE) }
    val parsed = BaseConverter.parse(sourceText, lastEdited)

    fun textFor(radix: Int) = if (radix == lastEdited) sourceText else BaseConverter.format(value, radix)

    fun edit(
        radix: Int,
        text: String,
    ) {
        sourceText = text
        lastEdited = radix
        BaseConverter.parse(text, radix)?.let { value = it }
    }

    LifeCalculatorScaffold(
        title = stringResource(R.string.basecalc_title),
        onOpenMenu = onOpenMenu,
    ) {
        LifeCalcCard {
            LifeCalcSectionLabel(stringResource(R.string.basecalc_section_values))
            BaseField(
                label = stringResource(R.string.basecalc_dec),
                value = textFor(BaseConverter.DEC),
                onValueChange = {
                    // Decimal: digit-only, accept negative not relevant
                    // (unsigned semantics).
                    edit(BaseConverter.DEC, it.filter { ch -> ch.isDigit() || ch == '_' })
                },
                keyboard = KeyboardType.Number,
            )
            BaseField(
                label = stringResource(R.string.basecalc_hex),
                value = textFor(BaseConverter.HEX),
                onValueChange = {
                    edit(BaseConverter.HEX, it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }.uppercase())
                },
                keyboard = KeyboardType.Text,
                monospace = true,
            )
            BaseField(
                label = stringResource(R.string.basecalc_oct),
                value = textFor(BaseConverter.OCT),
                onValueChange = {
                    edit(BaseConverter.OCT, it.filter { ch -> (ch in '0'..'7') || ch == '_' })
                },
                keyboard = KeyboardType.Number,
                monospace = true,
            )
            BaseField(
                label = stringResource(R.string.basecalc_bin),
                value = textFor(BaseConverter.BIN),
                onValueChange = {
                    edit(BaseConverter.BIN, it.filter { ch -> ch == '0' || ch == '1' || ch == '_' })
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

private const val DEFAULT_VALUE = 42L
