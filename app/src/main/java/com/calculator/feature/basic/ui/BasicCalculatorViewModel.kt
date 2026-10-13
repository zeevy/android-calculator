package com.calculator.feature.basic.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculator.core.common.format.messageRes
import com.calculator.core.data.history.HistoryEntry
import com.calculator.core.data.history.HistoryRepository
import com.calculator.core.data.settings.SettingsRepository
import com.calculator.core.data.settings.UserSettings
import com.calculator.core.data.tape.TapeEntry
import com.calculator.core.data.tape.TapeStore
import com.calculator.core.math.AngleMode
import com.calculator.core.math.EvaluationResult
import com.calculator.core.math.Evaluator
import com.calculator.core.math.ExpressionEditor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Holds the state of the basic calculator screen and forwards evaluation
 * to the [Evaluator] use-case.
 *
 * Design notes:
 *  - State is exposed as a single [StateFlow] of [BasicCalculatorUiState];
 *    the UI collects it with `collectAsStateWithLifecycle()`.
 *  - A fresh [Evaluator] is constructed per evaluation with the current
 *    [AngleMode] so the trig functions react to the DEG/RAD toggle without
 *    plumbing the mode through every call site.
 *  - The evaluation engine is synchronous and microsecond-fast for any
 *    realistic expression length, so no coroutine dispatch is needed.
 *
 * Input rules (operator collapse, decimals, leading zero, sign flip,
 * `=` completion) live in [ExpressionEditor], shared with the widget and
 * the floating calculator. On top of them this ViewModel adds:
 *  - Pressing `=` repeatedly re-applies the last `op + operand`.
 *  - Memory keys M+, M-, MR, MC operate on a single stored value.
 *
 * Process-death survival: expression, error, repeat token, scientific
 * toggle, angle mode, and memory are all persisted via [SavedStateHandle].
 */
@HiltViewModel
class BasicCalculatorViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val historyRepository: HistoryRepository,
        settingsRepository: SettingsRepository,
        private val tapeStore: TapeStore,
    ) : ViewModel() {
        // Precision flows in from SettingsRepository. Each `=` and live
        // preview pulls the current value, so a precision change in
        // Settings takes effect on the next keypress without restarting
        // anything.
        private val precision: StateFlow<Int> =
            settingsRepository.settings
                .map { it.precision }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = UserSettings.DEFAULT_PRECISION,
                )

        // Restore from the SavedStateHandle so calculator state survives
        // process death and config changes. `liveResult` is derived from
        // `expression`, so we recompute it rather than persist it.
        private val _state = MutableStateFlow(loadFromSavedState())
        val state: StateFlow<BasicCalculatorUiState> = _state.asStateFlow()

        /**
         * Session tape of committed equations, oldest first.
         *
         * Surfaced as its own flow rather than folded into
         * [BasicCalculatorUiState] because it lives in the shared
         * [TapeStore] - the display state is per-ViewModel, the tape is
         * not, and keeping them separate avoids copying the whole list
         * into a new state object on every keystroke.
         */
        val tape: StateFlow<List<TapeEntry>> = tapeStore.entries

        private fun loadFromSavedState(): BasicCalculatorUiState {
            val expression = savedStateHandle.get<String>(KEY_EXPRESSION).orEmpty()
            val angleMode =
                savedStateHandle
                    .get<String>(KEY_ANGLE_MODE)
                    ?.let { runCatching { AngleMode.valueOf(it) }.getOrNull() }
                    ?: AngleMode.Radian
            val memory =
                savedStateHandle
                    .get<String>(KEY_MEMORY)
                    ?.let { runCatching { BigDecimal(it) }.getOrNull() }
                    ?: BigDecimal.ZERO
            val restoredPreview = preview(expression, angleMode)
            return BasicCalculatorUiState(
                expression = expression,
                liveResult = restoredPreview,
                errorMessage = savedStateHandle.get<Int>(KEY_ERROR),
                pendingRepeat = savedStateHandle.get<String>(KEY_PENDING_REPEAT),
                scientific = savedStateHandle.get<Boolean>(KEY_SCIENTIFIC) ?: false,
                angleMode = angleMode,
                memory = memory,
                lastCommittedExpression = savedStateHandle.get<String>(KEY_LAST_COMMITTED),
                // Persisted snapshot of the last computable result. Falls
                // back to whatever the recomputed preview gives us if the
                // saved key is missing (older sessions / fresh install).
                lastValidPreview =
                    savedStateHandle.get<String>(KEY_LAST_PREVIEW) ?: restoredPreview,
            )
        }

        private fun persist(state: BasicCalculatorUiState) {
            savedStateHandle[KEY_EXPRESSION] = state.expression
            savedStateHandle[KEY_ERROR] = state.errorMessage
            savedStateHandle[KEY_PENDING_REPEAT] = state.pendingRepeat
            savedStateHandle[KEY_SCIENTIFIC] = state.scientific
            savedStateHandle[KEY_ANGLE_MODE] = state.angleMode.name
            savedStateHandle[KEY_MEMORY] = state.memory.toPlainString()
            savedStateHandle[KEY_LAST_COMMITTED] = state.lastCommittedExpression
            savedStateHandle[KEY_LAST_PREVIEW] = state.lastValidPreview
        }

        init {
            // Key sounds and haptics follow Settings live; they are not part
            // of the saved calculator state.
            viewModelScope.launch {
                settingsRepository.settings.collect { s ->
                    _state.update { it.copy(soundEnabled = s.sound, hapticsEnabled = s.haptics) }
                }
            }
        }

        /** Dispatch a user event - typically called from the UI on key press. */
        fun onEvent(event: BasicCalculatorEvent) {
            when (event) {
                is BasicCalculatorEvent.Append -> append(event.symbol)
                BasicCalculatorEvent.Backspace -> backspace()
                BasicCalculatorEvent.Clear -> clear()
                BasicCalculatorEvent.ClearAll -> {
                    // A deliberate press-and-hold on backspace means "blank
                    // sheet", so the inline tape goes with the expression.
                    // The persistent History sheet is untouched - that is
                    // the durable record.
                    tapeStore.clear()
                    clear()
                }
                BasicCalculatorEvent.Equals -> commit()
                BasicCalculatorEvent.ToggleScientific -> toggleScientific()
                is BasicCalculatorEvent.SetScientific ->
                    if (_state.value.scientific != event.enabled) toggleScientific()
                is BasicCalculatorEvent.Reuse -> {
                    clear()
                    append(event.expression)
                }
                BasicCalculatorEvent.ToggleAngleMode -> toggleAngleMode()
                BasicCalculatorEvent.MemoryAdd -> memoryDelta(addToMemory = true)
                BasicCalculatorEvent.MemorySubtract -> memoryDelta(addToMemory = false)
                BasicCalculatorEvent.MemoryRecall -> memoryRecall()
                BasicCalculatorEvent.MemoryClear -> memoryClear()
                BasicCalculatorEvent.SignFlip -> signFlip()
                is BasicCalculatorEvent.DeleteTapeLine -> tapeStore.remove(event.id)
            }
        }

        private fun append(symbol: String) =
            _state.update { current ->
                val next = ExpressionEditor.append(current.expression, symbol, afterEquals = current.pendingRepeat != null)
                val newPreview = preview(next, current.angleMode)
                current
                    .copy(
                        expression = next,
                        liveResult = newPreview,
                        errorMessage = null,
                        // Any non-`=` input breaks the repeat-equals chain.
                        pendingRepeat = null,
                        // The user is moving on - either starting a fresh
                        // expression (digit/decimal after =) or chaining a
                        // new computation on top of the previous result
                        // (operator after =). Either way, drop the
                        // "what produced the result" line; the new
                        // expression is what matters now.
                        lastCommittedExpression = null,
                        // Hold the previous result across intermediate
                        // non-computable states (e.g. `5+3` -> `5+3+`).
                        // [holdPreview] clears when the expression itself
                        // is wiped to nothing.
                        lastValidPreview = holdPreview(next, newPreview, current.lastValidPreview),
                    ).also(::persist)
            }

        private fun backspace() =
            _state.update { current ->
                val next = current.expression.dropLast(1)
                val newPreview = preview(next, current.angleMode)
                current
                    .copy(
                        expression = next,
                        liveResult = newPreview,
                        errorMessage = null,
                        pendingRepeat = null,
                        // Backspace edits the expression; the previously-
                        // committed "history" line no longer corresponds to
                        // what's on screen, so drop it.
                        lastCommittedExpression = null,
                        lastValidPreview = holdPreview(next, newPreview, current.lastValidPreview),
                    ).also(::persist)
            }

        private fun clear() =
            _state.update { current ->
                // Clear wipes the expression and error/repeat, but keeps user
                // preferences (scientific mode, angle mode, memory, key feedback).
                BasicCalculatorUiState(
                    scientific = current.scientific,
                    angleMode = current.angleMode,
                    memory = current.memory,
                    soundEnabled = current.soundEnabled,
                    hapticsEnabled = current.hapticsEnabled,
                ).also(::persist)
            }

        private fun commit() {
            // update() may run its block more than once when it races, so the
            // block only computes the new state; the tape and history writes
            // happen once, after it.
            var record: (() -> Unit)? = null
            _state.update { current ->
                record = null
                if (current.expression.isBlank()) return@update current

                val (toEvaluate, repeatToken) = buildEquationToEvaluate(current)

                when (val result = evaluatorFor(current.angleMode).evaluate(toEvaluate)) {
                    is EvaluationResult.Success -> {
                        val canonicalResult = ExpressionEditor.resultText(result.value)
                        // An identity commit computed nothing, so it earns no
                        // line in either sink. This is what `=` does on a bare
                        // number: `5+6 =` leaves `11` on the display, and every
                        // further press just re-evaluates `11` to `11`. Only a
                        // literal string match counts as an identity, so real
                        // arithmetic that happens to be a no-op numerically
                        // (`5×1`, `5+0`) still records.
                        //
                        // A repeat-equals chain is unaffected: its equation
                        // moves on with every press (`2+2`, `4+2`, `6+2`), so
                        // none of those are identities.
                        val isIdentity = toEvaluate == canonicalResult
                        if (!isIdentity) {
                            record = {
                                recordCommit(
                                    expression = toEvaluate,
                                    result = canonicalResult,
                                    scientific = current.scientific,
                                    isRepeat = current.pendingRepeat != null,
                                )
                            }
                        }
                        current
                            .copy(
                                // Replace the expression with the canonical result so the
                                // user can keep chaining (e.g. press `+` after `=`).
                                // `stripTrailingZeros` collapses `4.0` to `4` for display,
                                // and `toPlainString` keeps it out of scientific notation so
                                // the next keypress chains naturally.
                                expression = canonicalResult,
                                liveResult = null,
                                errorMessage = null,
                                pendingRepeat = repeatToken,
                                // Preserve the typed expression so the display's top
                                // line keeps showing what was committed even after the
                                // bottom line collapses to the canonical result.
                                lastCommittedExpression = toEvaluate,
                                // The committed result IS the most recent valid
                                // preview - record it so chained operators
                                // after `=` continue showing it on the result row.
                                lastValidPreview = canonicalResult,
                            ).also(::persist)
                    }

                    is EvaluationResult.Error ->
                        current
                            .copy(
                                liveResult = null,
                                errorMessage = result.messageRes(),
                                pendingRepeat = null,
                                // Failed equations don't get an history line - the
                                // expression that's still on screen IS the input,
                                // and the bottom row will surface the error.
                                lastCommittedExpression = null,
                                // Keep the previously-known good preview - the
                                // error message takes precedence in the UI, but
                                // recovering from the error shouldn't blank the
                                // result row.
                            ).also(::persist)
                }
            }
            record?.invoke()
        }

        /**
         * Fan a successful `=` out to the session tape and the durable
         * history.
         *
         * The two sinks deliberately disagree about repeat-equals
         * replays. Every `=` prints a tape line the way a desk
         * calculator's paper roll does - the display no longer echoes
         * the committed expression, so without a line per replay a
         * `1 + 5 = = =` chain would show `16` with nothing saying where
         * it came from. The durable history skips replays so the table
         * isn't filled with near-identical rows.
         *
         * @param isRepeat Whether this `=` was a replay of the stored
         *   `op + operand` rather than a freshly typed equation.
         */
        private fun recordCommit(
            expression: String,
            result: String,
            scientific: Boolean,
            isRepeat: Boolean,
        ) {
            tapeStore.add(expression, result)
            if (isRepeat) return
            // Best-effort and fire-and-forget: a failed insert is
            // non-fatal because the user already saw their result.
            viewModelScope.launch {
                runCatching {
                    historyRepository.add(
                        expression = expression,
                        result = result,
                        type = if (scientific) HistoryEntry.Type.Scientific else HistoryEntry.Type.Basic,
                    )
                }
            }
        }

        private fun toggleScientific() =
            _state.update { current ->
                current.copy(scientific = !current.scientific).also(::persist)
            }

        private fun toggleAngleMode() =
            _state.update { current ->
                val next =
                    when (current.angleMode) {
                        AngleMode.Radian -> AngleMode.Degree
                        AngleMode.Degree -> AngleMode.Radian
                    }
                current
                    .copy(
                        angleMode = next,
                        liveResult = preview(current.expression, next),
                    ).also(::persist)
            }

        /** Add or subtract the current display value to the stored memory. */
        private fun memoryDelta(addToMemory: Boolean) =
            _state.update { current ->
                val currentValue = currentDisplayValue(current) ?: return@update current
                val nextMemory =
                    if (addToMemory) current.memory + currentValue else current.memory - currentValue
                current.copy(memory = nextMemory).also(::persist)
            }

        private fun memoryRecall() =
            _state.update { current ->
                // Empty memory means M+/M- was never pressed (or MC was).
                // Recalling it would just append "0" - and on the next
                // tap "×0", building "0×0×0×..." indefinitely. Treat it
                // as a no-op instead so MR is harmless before any value
                // is stored.
                if (current.memory.signum() == 0) return@update current
                val mem = ExpressionEditor.resultText(current.memory)
                val next =
                    when {
                        current.pendingRepeat != null -> mem
                        current.expression.isEmpty() -> mem
                        current.expression.last() in ExpressionEditor.ArithmeticOperators ||
                            current.expression.last() == '(' -> current.expression + mem
                        else -> current.expression + "×" + mem
                    }
                val newPreview = preview(next, current.angleMode)
                current
                    .copy(
                        expression = next,
                        liveResult = newPreview,
                        errorMessage = null,
                        pendingRepeat = null,
                        // MR rewrites the expression; the old history line no
                        // longer matches what's on screen.
                        lastCommittedExpression = null,
                        lastValidPreview = holdPreview(next, newPreview, current.lastValidPreview),
                    ).also(::persist)
            }

        private fun memoryClear() =
            _state.update { current ->
                current.copy(memory = BigDecimal.ZERO).also(::persist)
            }

        /**
         * Toggle the sign of the trailing numeric operand in the expression.
         *
         * Examples (`|` marks where the user's caret would be):
         *  - `5|`         -> `-5|`
         *  - `-5|`        -> `5|`
         *  - `10+5|`      -> `10+-5|`   (the engine parses `+-5` as `+(-5)`)
         *  - `10+-5|`     -> `10+5|`
         *  - `|` (empty)  -> `-|`       (so the next digit lands as `-digit`)
         *  - `10+|`       -> `10+-|`    (no operand yet; arm a unary minus
         *                                for the next digit)
         *
         * After equals, the expression IS the result, so `signFlip` just
         * flips its sign - same code path because the result is a single
         * numeric token.
         */
        private fun signFlip() =
            _state.update { current ->
                val flipped = ExpressionEditor.flipSign(current.expression)
                val newPreview = preview(flipped, current.angleMode)
                current
                    .copy(
                        expression = flipped,
                        liveResult = newPreview,
                        errorMessage = null,
                        pendingRepeat = null,
                        // SignFlip rewrites the expression in place; the
                        // previous history line no longer corresponds to
                        // what's on screen.
                        lastCommittedExpression = null,
                        lastValidPreview = holdPreview(flipped, newPreview, current.lastValidPreview),
                    ).also(::persist)
            }

        /**
         * Resolve the canonical equation string to evaluate on `=`.
         *
         * Returns `(expressionToEvaluate, repeatTokenForNextEquals)`. The
         * repeat token is the trailing `op+operand` to be re-applied when
         * the user keeps hitting `=`.
         */
        private fun buildEquationToEvaluate(current: BasicCalculatorUiState): Pair<String, String?> {
            // Replay path: previous `=` set a repeat token, and the user
            // pressed `=` again with the result still on the display.
            current.pendingRepeat?.let { repeat ->
                return (current.expression + repeat) to repeat
            }

            return ExpressionEditor.completeForEquals(current.expression)
        }

        /**
         * Compute a non-binding preview result.
         *
         * Errors during preview are intentionally swallowed: the user is still
         * typing, and partial expressions like `5 +` should not light up
         * an error banner mid-input.
         */
        private fun preview(expression: String, angleMode: AngleMode): String? {
            if (expression.isBlank()) return null
            return when (val result = evaluatorFor(angleMode).evaluate(expression)) {
                is EvaluationResult.Success -> ExpressionEditor.resultText(result.value)
                is EvaluationResult.Error -> null
            }
        }

        /**
         * Resolve the `lastValidPreview` value to persist on this state
         * update.
         *
         * Rules:
         *  - Expression went blank (Clear, Backspace-to-empty) → clear
         *    the held preview; there's no "previous result" to remember.
         *  - New preview is computable → it becomes the new held value.
         *  - New preview is null but expression is non-blank → keep the
         *    previous held value so the bottom row doesn't blank out
         *    mid-typing (`5+3` → `5+3+` still shows the `8` underneath).
         */
        private fun holdPreview(
            nextExpression: String,
            newPreview: String?,
            previousHeld: String?,
        ): String? =
            when {
                nextExpression.isBlank() -> null
                newPreview != null -> newPreview
                else -> previousHeld
            }

        /** Build a fresh evaluator pinned to the current angle mode. */
        private fun evaluatorFor(angleMode: AngleMode): Evaluator =
            Evaluator(
                mathContext = MathContext(precision.value, RoundingMode.HALF_EVEN),
                angleMode = angleMode,
            )

        /** Best-effort current numeric value: the live result if available, else null. */
        private fun currentDisplayValue(state: BasicCalculatorUiState): BigDecimal? =
            state.liveResult?.let { runCatching { BigDecimal(it) }.getOrNull() }
                ?: runCatching { BigDecimal(state.expression) }.getOrNull()

        private companion object {
            const val KEY_EXPRESSION = "calculator.expression"
            const val KEY_ERROR = "calculator.error"
            const val KEY_PENDING_REPEAT = "calculator.pendingRepeat"
            const val KEY_SCIENTIFIC = "calculator.scientific"
            const val KEY_ANGLE_MODE = "calculator.angleMode"
            const val KEY_MEMORY = "calculator.memory"
            const val KEY_LAST_COMMITTED = "calculator.lastCommittedExpression"
            const val KEY_LAST_PREVIEW = "calculator.lastValidPreview"
        }
    }
