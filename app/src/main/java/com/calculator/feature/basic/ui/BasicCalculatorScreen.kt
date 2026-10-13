package com.calculator.feature.basic.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calculator.R
import com.calculator.core.data.tape.TapeEntry
import com.calculator.core.designsystem.theme.CalculatorTheme
import com.calculator.core.math.AngleMode
import java.math.BigDecimal

/**
 * Top-level composable for the basic calculator screen.
 *
 * Pulls the [BasicCalculatorViewModel] from Hilt and renders the
 * stateless [BasicCalculatorScreenContent] - this split keeps previews
 * possible without a ViewModel and makes UI tests straightforward.
 */
@Composable
fun BasicCalculatorScreen(
    onOpenMenu: () -> Unit = {},
    viewModel: BasicCalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tape by viewModel.tape.collectAsStateWithLifecycle()
    BasicCalculatorScreenContent(
        state = state,
        tape = tape,
        onEvent = viewModel::onEvent,
        onOpenMenu = onOpenMenu,
    )
}

/**
 * Stateless screen content.
 *
 * Decoupling state from the ViewModel here is the single most useful
 * thing for testability: previews and Compose UI tests construct a
 * [BasicCalculatorUiState] directly and pass a no-op `onEvent` lambda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BasicCalculatorScreenContent(
    state: BasicCalculatorUiState,
    onEvent: (BasicCalculatorEvent) -> Unit,
    onOpenMenu: () -> Unit = {},
    tape: List<TapeEntry> = emptyList(),
) {
    val tones = rememberKeyToneGenerator()
    val tonesIfEnabled = if (state.soundEnabled) tones else null

    CompositionLocalProvider(
        LocalKeyTones provides tonesIfEnabled,
        LocalHapticsEnabled provides state.hapticsEnabled,
    ) {
        Scaffold(
            // Hand out insets per-child: outer Column absorbs the status-bar
            // inset (so the display card sits below the system icons), and
            // the keypad card absorbs the navigation-bar inset on its bottom
            // edge (so the bottom row of keys doesn't collide with the
            // gesture pill).
            contentWindowInsets = WindowInsets(0.dp),
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp),
            ) {
                // Display sits directly on the screen background - no card,
                // no border. The typographic hierarchy inside (small muted
                // expression line, big bold result) carries the visual
                // weight on its own, and the gap before the keypad tray
                // does the section separation.
                DisplaySection(
                    state = state,
                    tape = tape,
                    onEvent = onEvent,
                    onOpenMenu = onOpenMenu,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )
                Spacer(Modifier.size(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                )
                Spacer(Modifier.size(12.dp))
                // No keypad tray. iOS calculator puts keys directly on the
                // screen background; the colored keys carry the visual
                // structure on their own.
                Keypad(
                    scientific = state.scientific,
                    onEvent = onEvent,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 12.dp),
                )
            }
        }
    } // CompositionLocalProvider
}

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
private fun ScientificCalculatorPreview() {
    CalculatorTheme {
        Surface {
            BasicCalculatorScreenContent(
                state =
                    BasicCalculatorUiState(
                        expression = "sin(30)+log(100)",
                        liveResult = "2.5",
                        scientific = true,
                        angleMode = AngleMode.Degree,
                        memory = BigDecimal("42"),
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
                        errorMessage = R.string.basic_error_division_by_zero,
                    ),
                onEvent = {},
            )
        }
    }
}
