package com.calculator.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calculator.R
import com.calculator.core.data.settings.DataStoreSettingsRepository
import com.calculator.core.data.settings.UserSettings
import com.calculator.feature.floating.FloatingCalculatorService

/**
 * Settings sheet shown when the user taps the Settings tile in the tools
 * menu. Lists the persisted user preferences with iOS-style controls
 * (rounded rows, accent-colored switches, segmented theme picker).
 *
 * Writes go straight through the ViewModel into [SettingsRepository] -
 * Compose collects the resulting StateFlow so the UI always reflects
 * the latest persisted state.
 */
@Composable
fun SettingsSheetContent(
    // Kept in the API so the host bottom sheet can wire dismiss into its
    // own state; the sheet itself has no in-content close affordance.
    @Suppress("UnusedParameter") onClose: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val accent = SettingsAccent
    val context = LocalContext.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
        )

        // Theme picker - segmented "System / Light / Dark".
        SectionLabel(stringResource(R.string.settings_section_appearance))
        SegmentedThemePicker(
            selected = settings.theme,
            onSelect = viewModel::setTheme,
            accent = accent,
        )
        Spacer(Modifier.size(8.dp))
        ToggleRow(
            label = stringResource(R.string.settings_dynamic_color),
            description = stringResource(R.string.settings_dynamic_color_desc),
            checked = settings.dynamicColor,
            onCheckedChange = viewModel::setDynamicColor,
            accent = accent,
        )

        // Feedback.
        Spacer(Modifier.size(16.dp))
        SectionLabel(stringResource(R.string.settings_section_feedback))
        ToggleRow(
            label = stringResource(R.string.settings_haptics),
            description = stringResource(R.string.settings_haptics_desc),
            checked = settings.haptics,
            onCheckedChange = viewModel::setHaptics,
            accent = accent,
        )
        Spacer(Modifier.size(4.dp))
        ToggleRow(
            label = stringResource(R.string.settings_sound),
            description = stringResource(R.string.settings_sound_desc),
            checked = settings.sound,
            onCheckedChange = viewModel::setSound,
            accent = accent,
        )

        // Precision slider.
        Spacer(Modifier.size(16.dp))
        SectionLabel(stringResource(R.string.settings_section_precision))
        PrecisionRow(
            precision = settings.precision,
            onChange = viewModel::setPrecision,
            accent = accent,
        )

        // Floating calculator.
        Spacer(Modifier.size(16.dp))
        SectionLabel(stringResource(R.string.settings_section_floating))
        ActionRow(
            label = stringResource(R.string.settings_floating_launch),
            description = stringResource(R.string.settings_floating_desc),
            onClick = { FloatingCalculatorService.startOrRequestPermission(context) },
        )

        // Privacy.
        Spacer(Modifier.size(16.dp))
        SectionLabel(stringResource(R.string.settings_section_privacy))
        ToggleRow(
            label = stringResource(R.string.settings_crash),
            description = stringResource(R.string.settings_crash_desc),
            checked = settings.crashOptIn,
            onCheckedChange = viewModel::setCrashOptIn,
            accent = accent,
        )

        // About.
        Spacer(Modifier.size(16.dp))
        SectionLabel(stringResource(R.string.settings_section_about))
        AboutRow(
            label = stringResource(R.string.settings_about_version),
            value = stringResource(R.string.settings_about_version_value),
        )
        AboutRow(
            label = stringResource(R.string.settings_about_license),
            value = stringResource(R.string.settings_about_license_value),
        )
        AboutRow(
            label = stringResource(R.string.settings_about_github),
            value = stringResource(R.string.settings_about_github_value),
        )

        Spacer(Modifier.size(24.dp))
    }
}

/**
 * Small uppercase header above each settings group (Appearance,
 * Feedback, etc.). Matches the iOS Settings app convention; uppercase
 * is done at render time so callers pass plain title-case strings.
 */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
    )
}

/**
 * Tappable row with a label, optional description, and trailing switch.
 *
 * The whole row is clickable, not just the switch - matches the iOS
 * pattern where tapping anywhere on the cell toggles the value. The
 * switch's own `onCheckedChange` covers the case where the user drags
 * the thumb instead of tapping.
 *
 * @param label Primary row label.
 * @param description Optional second line; pass null to skip.
 * @param checked Current switch state.
 * @param onCheckedChange Invoked with the new value when the row or
 *   switch is tapped.
 * @param accent Track colour when checked - reused by the slider and
 *   theme picker so the whole sheet has one accent.
 */
@Composable
private fun ToggleRow(
    label: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RowBackground)
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = UncheckedTrackColor,
                    uncheckedBorderColor = Color.Transparent,
                ),
        )
    }
}

/**
 * Tappable row that fires an action (no persistent toggle). Same chrome
 * as [ToggleRow] minus the trailing switch so the two read as the same
 * family of cells in the settings sheet.
 */
@Composable
private fun ActionRow(
    label: String,
    description: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RowBackground)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun SegmentedThemePicker(
    selected: UserSettings.ThemeOption,
    onSelect: (UserSettings.ThemeOption) -> Unit,
    accent: Color,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RowBackground)
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        UserSettings.ThemeOption.entries.forEach { option ->
            val isSelected = option == selected
            Text(
                text = stringResource(option.labelRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.Black else Color.White,
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) accent else Color.Transparent)
                        .clickable { onSelect(option) }
                        .padding(vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/**
 * Significant-figures slider.
 *
 * Steps are the integer count between
 * [DataStoreSettingsRepository.MIN_PRECISION] and
 * [DataStoreSettingsRepository.MAX_PRECISION]. Slider's `steps` value
 * is "tick marks between min and max" (exclusive of both endpoints),
 * so it's `(max - min) - 1`.
 *
 * Picked-up changes flow through [DataStoreSettingsRepository] and the
 * Evaluator instances pull the new precision on their next construction
 * - precision changes take effect on the next keypress, no restart.
 */
@Composable
private fun PrecisionRow(
    precision: Int,
    onChange: (Int) -> Unit,
    accent: Color,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RowBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_precision_label),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = precision.toString(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = accent,
            )
        }
        Slider(
            value = precision.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange =
                DataStoreSettingsRepository.MIN_PRECISION.toFloat()..DataStoreSettingsRepository.MAX_PRECISION.toFloat(),
            steps =
                DataStoreSettingsRepository.MAX_PRECISION -
                    DataStoreSettingsRepository.MIN_PRECISION - 1,
            colors =
                SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
        )
        Text(
            text = stringResource(R.string.settings_precision_description),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

private fun UserSettings.ThemeOption.labelRes(): Int =
    when (this) {
        UserSettings.ThemeOption.System -> R.string.settings_theme_system
        UserSettings.ThemeOption.Light -> R.string.settings_theme_light
        UserSettings.ThemeOption.Dark -> R.string.settings_theme_dark
    }

// Background tint for settings rows. Slightly lighter than the
// sheet container so individual rows read as distinct cards.
private val RowBackground = Color(0xFF2C2C2E)
private val UncheckedTrackColor = Color(0xFF555555)

// Same orange as the keypad operator keys. Reused here so the settings
// sheet stays visually consistent with the rest of the app.
private val SettingsAccent = Color(0xFFFF9F0A)

@Suppress("unused")
private val PaddingPlaceholder = PaddingValues(0.dp)
