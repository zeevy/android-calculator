package com.calculator.core.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.calculator.core.data.di.SettingsEntryPoint
import com.calculator.core.math.AngleMode
import com.calculator.core.math.EvaluationResult
import com.calculator.core.math.Evaluator
import com.calculator.core.math.ExpressionEditor
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.math.MathContext
import java.math.RoundingMode
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * A pocket-calculator-shaped home-screen widget.
 *
 * State is one [String] - the current expression - stored in the
 * widget's own DataStore Preferences via Glance's
 * [PreferencesGlanceStateDefinition]. Every key tap routes through
 * [WidgetKeyAction], which mutates the expression and triggers a
 * recomposition.
 *
 * The widget runs the same [Evaluator] the main app uses (no special
 * widget-only math path) - pressing `=` evaluates the expression and
 * replaces it with the canonical result.
 */
class QuickCalcWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }
}

/**
 * Broadcast receiver glue declared in the manifest. Glance routes
 * `APPWIDGET_UPDATE` and `APPWIDGET_DELETED` broadcasts here, which
 * then delegates to [QuickCalcWidget] for the actual rendering.
 */
class QuickCalcWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickCalcWidget()
}

// DataStore key for the widget's persisted expression. One key (and
// therefore one widget state) is sufficient because every instance of
// the widget on the home screen is independent - Glance assigns each
// its own DataStore file behind the scenes keyed by GlanceId.
private val ExpressionKey = stringPreferencesKey("widget.expression")

// True while the expression is a result just produced by `=`, so the next
// digit starts over (the same rule as the app's keypad).
private val AfterEqualsKey = booleanPreferencesKey("widget.afterEquals")

private val OperatorColor = ComposeColor(0xFFFF9F0A)
private val OnOperatorColor = ComposeColor.White
private val DigitColor = ComposeColor(0xFF505050)
private val ModifierColor = ComposeColor(0xFFA5A5A5)
private val DisplayBackground = ComposeColor(0xFF1C1C1E)
private val WidgetBackground = ComposeColor.Black

@androidx.compose.runtime.Composable
private fun WidgetContent() {
    val prefs = currentState<Preferences>()
    val expression = prefs[ExpressionKey].orEmpty()

    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(WidgetBackground)
                .cornerRadius(20.dp)
                .padding(8.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.End,
    ) {
        // Display row: shows the current expression, or "0" when empty.
        Box(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .cornerRadius(12.dp)
                    .background(DisplayBackground)
                    .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = expression.ifEmpty { "0" },
                maxLines = 1,
                style =
                    TextStyle(
                        color = ColorProvider(ComposeColor.White),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                    ),
            )
        }
        Spacer(GlanceModifier.height(6.dp))

        // 4x4 keypad. Rows are weighted via fixed heights inside a
        // Column-of-Rows because Glance doesn't support Modifier.weight
        // the way regular Compose does.
        val keypad =
            listOf(
                listOf(WKey("C", KeyKind.Modifier), WKey("(", KeyKind.Modifier), WKey(")", KeyKind.Modifier), WKey("÷", KeyKind.Operator)),
                listOf(WKey("7", KeyKind.Digit), WKey("8", KeyKind.Digit), WKey("9", KeyKind.Digit), WKey("×", KeyKind.Operator)),
                listOf(WKey("4", KeyKind.Digit), WKey("5", KeyKind.Digit), WKey("6", KeyKind.Digit), WKey("-", KeyKind.Operator)),
                listOf(WKey("1", KeyKind.Digit), WKey("2", KeyKind.Digit), WKey("3", KeyKind.Digit), WKey("+", KeyKind.Operator)),
                listOf(WKey("⌫", KeyKind.Modifier), WKey("0", KeyKind.Digit), WKey(".", KeyKind.Digit), WKey("=", KeyKind.Equals)),
            )
        keypad.forEach { row ->
            Row(modifier = GlanceModifier.fillMaxWidth().height(40.dp)) {
                row.forEachIndexed { idx, key ->
                    KeyBox(key = key)
                    if (idx != row.lastIndex) Spacer(GlanceModifier.width(4.dp))
                }
            }
            Spacer(GlanceModifier.height(4.dp))
        }
    }
}

private enum class KeyKind { Digit, Operator, Modifier, Equals }

private data class WKey(val label: String, val kind: KeyKind)

@androidx.compose.runtime.Composable
private fun KeyBox(key: WKey) {
    val (bg, fg) =
        when (key.kind) {
            KeyKind.Digit -> DigitColor to ComposeColor.White
            KeyKind.Operator -> OperatorColor to OnOperatorColor
            KeyKind.Modifier -> ModifierColor to ComposeColor.Black
            KeyKind.Equals -> OperatorColor to OnOperatorColor
        }
    Box(
        modifier =
            GlanceModifier
                // width set via Row distribution (no weight in Glance;
                // we use the row's intrinsic equal-distribution).
                .height(40.dp)
                .width(60.dp)
                .cornerRadius(12.dp)
                .background(bg)
                .clickable(
                    actionRunCallback<WidgetKeyAction>(
                        actionParametersOf(WidgetKeyAction.LABEL_KEY to key.label),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key.label,
            style =
                TextStyle(
                    color = ColorProvider(fg),
                    fontSize = 18.sp,
                    fontWeight =
                        if (key.kind == KeyKind.Equals) FontWeight.Bold else FontWeight.Medium,
                ),
        )
    }
}

/**
 * Handles a single widget key press with the app's own input rules
 * ([ExpressionEditor]) and [Evaluator], at the precision chosen in
 * Settings, so widget math behaves exactly like the in-app calculator.
 */
class WidgetKeyAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val label = parameters[LABEL_KEY] ?: return
        val precision =
            EntryPointAccessors
                .fromApplication(context, SettingsEntryPoint::class.java)
                .settingsRepository()
                .settings
                .first()
                .precision
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[ExpressionKey].orEmpty()
            val afterEquals = prefs[AfterEqualsKey] ?: false
            when (label) {
                "C" -> prefs[ExpressionKey] = ""
                "⌫" -> prefs[ExpressionKey] = current.dropLast(1)
                "=" -> {
                    val result = evaluate(current, precision)
                    prefs[ExpressionKey] = result ?: current
                    prefs[AfterEqualsKey] = result != null
                    return@updateAppWidgetState
                }
                else -> prefs[ExpressionKey] = ExpressionEditor.append(current, label, afterEquals)
            }
            prefs[AfterEqualsKey] = false
        }
        QuickCalcWidget().update(context, glanceId)
    }

    /**
     * Evaluate the expression the way `=` does in the app (closing open
     * parentheses, completing a trailing operator).
     *
     * @return the result, or null on an error. The expression is then
     *   left unchanged rather than wiped - the user just typed it and
     *   can fix the typo and press `=` again.
     */
    private fun evaluate(
        expression: String,
        precision: Int,
    ): String? {
        if (expression.isBlank()) return null
        val (toEvaluate, _) = ExpressionEditor.completeForEquals(expression)
        // Radian mode is fixed because the widget has no scientific keys.
        val evaluator =
            Evaluator(mathContext = MathContext(precision, RoundingMode.HALF_EVEN), angleMode = AngleMode.Radian)
        return when (val result = evaluator.evaluate(toEvaluate)) {
            is EvaluationResult.Success -> ExpressionEditor.resultText(result.value)
            is EvaluationResult.Error -> null
        }
    }

    companion object {
        val LABEL_KEY = ActionParameters.Key<String>("label")
    }
}
