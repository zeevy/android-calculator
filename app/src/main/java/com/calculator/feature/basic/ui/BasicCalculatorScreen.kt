package com.calculator.feature.basic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calculator.core.designsystem.theme.CalculatorTheme

/**
 * Top-level composable for the basic calculator screen.
 *
 * Pulls the [BasicCalculatorViewModel] from Hilt and renders the
 * stateless [BasicCalculatorScreenContent] - this split keeps previews
 * possible without a ViewModel and makes UI tests straightforward.
 */
@Composable
fun BasicCalculatorScreen(viewModel: BasicCalculatorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BasicCalculatorScreenContent(state = state, onEvent = viewModel::onEvent)
}

/**
 * Stateless screen content.
 *
 * Decoupling state from the ViewModel here is the single most useful
 * thing for testability: previews and Compose UI tests construct a
 * [BasicCalculatorUiState] directly and pass a no-op `onEvent` lambda.
 */
@Composable
internal fun BasicCalculatorScreenContent(
    state: BasicCalculatorUiState,
    onEvent: (BasicCalculatorEvent) -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .systemBarsPadding()
                    .padding(16.dp),
        ) {
            Display(
                expression = state.expression,
                preview = state.liveResult,
                error = state.errorMessage,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
            Spacer(Modifier.size(24.dp))
            Keypad(onEvent = onEvent)
        }
    }
}

/**
 * Result display: the entered expression up top, with a smaller live
 * preview (or error) beneath it.
 */
@Composable
private fun Display(
    expression: String,
    preview: String?,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = expression.ifBlank { "0" },
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.End,
            )
            when {
                error != null ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                preview != null ->
                    Text(
                        text = "= $preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
        }
    }
}

/**
 * 4x5 keypad. Layout is intentionally hard-coded here for the scaffold;
 * a future iteration will pull key descriptors from a data class so the
 * keypad can be reconfigured for scientific mode and landscape.
 */
@Composable
private fun Keypad(onEvent: (BasicCalculatorEvent) -> Unit) {
    val rows =
        listOf(
            listOf(Key.Clear, Key.LeftParen, Key.RightParen, Key.Symbol("÷")),
            listOf(Key.Symbol("7"), Key.Symbol("8"), Key.Symbol("9"), Key.Symbol("×")),
            listOf(Key.Symbol("4"), Key.Symbol("5"), Key.Symbol("6"), Key.Symbol("-")),
            listOf(Key.Symbol("1"), Key.Symbol("2"), Key.Symbol("3"), Key.Symbol("+")),
            listOf(Key.Backspace, Key.Symbol("0"), Key.Symbol("."), Key.Equals),
        )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    KeyButton(
                        key = key,
                        onEvent = onEvent,
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(KEY_ASPECT_RATIO),
                    )
                }
            }
        }
    }
}

/**
 * Sealed key descriptor.
 *
 * Each variant knows how to render itself and which [BasicCalculatorEvent]
 * to dispatch, so the [Keypad] does not need a `when` over every label.
 */
private sealed interface Key {
    val label: String

    /** A character key (digit, operator, parenthesis). */
    data class Symbol(
        override val label: String,
    ) : Key

    data object LeftParen : Key {
        override val label: String = "("
    }

    data object RightParen : Key {
        override val label: String = ")"
    }

    data object Clear : Key {
        override val label: String = "C"
    }

    data object Backspace : Key {
        override val label: String = "⌫"
    }

    data object Equals : Key {
        override val label: String = "="
    }
}

@Composable
private fun KeyButton(
    key: Key,
    onEvent: (BasicCalculatorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val click: () -> Unit = {
        when (key) {
            is Key.Symbol -> onEvent(BasicCalculatorEvent.Append(key.label))
            Key.LeftParen -> onEvent(BasicCalculatorEvent.Append("("))
            Key.RightParen -> onEvent(BasicCalculatorEvent.Append(")"))
            Key.Clear -> onEvent(BasicCalculatorEvent.Clear)
            Key.Backspace -> onEvent(BasicCalculatorEvent.Backspace)
            Key.Equals -> onEvent(BasicCalculatorEvent.Equals)
        }
    }

    // Rounded-square keys: 20dp radius keeps the soft Material 3 feel while
    // reading as a square rather than the default near-circular pill shape
    // that Material 3 buttons fall into at small aspect-ratio sizes.
    val keyShape = RoundedCornerShape(20.dp)

    // Operators and the equals key get a larger type ramp than digits so the
    // calculator's "action" keys read as visually heavier.
    val labelStyle = when {
        key is Key.Symbol && key.label in OperatorLabels -> MaterialTheme.typography.displaySmall
        key is Key.Equals -> MaterialTheme.typography.displaySmall
        else -> MaterialTheme.typography.headlineLarge
    }

    when (key) {
        Key.Equals ->
            Button(
                onClick = click,
                modifier = modifier,
                shape = keyShape,
                contentPadding = PaddingValues(0.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) { Text(key.label, style = labelStyle) }

        else ->
            FilledTonalButton(
                onClick = click,
                modifier = modifier,
                shape = keyShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(key.label, style = labelStyle)
            }
    }
}

/** Labels of arithmetic-operator keys that get the bigger display type ramp. */
private val OperatorLabels = setOf("+", "-", "×", "÷", "%")

/**
 * Width-to-height ratio for each keypad button. >1 means wider than tall,
 * giving the keypad a shorter, more rectangular row.
 */
private const val KEY_ASPECT_RATIO = 1.5f

// ----- Previews -----

@PreviewLightDark
@Composable
private fun BasicCalculatorPreview() {
    CalculatorTheme {
        Surface {
            BasicCalculatorScreenContent(
                state =
                    BasicCalculatorUiState(
                        expression = "12 × 3 + 4",
                        liveResult = "40",
                    ),
                onEvent = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BasicCalculatorErrorPreview() {
    CalculatorTheme {
        Surface {
            BasicCalculatorScreenContent(
                state =
                    BasicCalculatorUiState(
                        expression = "5 ÷ 0",
                        errorMessage = "Can't divide by zero",
                    ),
                onEvent = {},
            )
        }
    }
}
