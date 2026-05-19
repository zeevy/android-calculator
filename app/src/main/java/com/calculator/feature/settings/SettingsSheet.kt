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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calculator.core.data.settings.DataStoreSettingsRepository
import com.calculator.core.data.settings.UserSettings

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
    onClose: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val accent = SettingsAccent

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
        )

        // Theme picker - segmented "System / Light / Dark".
        SectionLabel("Appearance")
        SegmentedThemePicker(
            selected = settings.theme,
            onSelect = viewModel::setTheme,
            accent = accent,
        )
        Spacer(Modifier.size(8.dp))
        ToggleRow(
            label = "Dynamic color",
            description = "Use the wallpaper's colors (Android 12+).",
            checked = settings.dynamicColor,
            onCheckedChange = viewModel::setDynamicColor,
            accent = accent,
        )

        // Feedback.
        Spacer(Modifier.size(16.dp))
        SectionLabel("Feedback")
        ToggleRow(
            label = "Haptics",
            description = "Vibrate on key press.",
            checked = settings.haptics,
            onCheckedChange = viewModel::setHaptics,
            accent = accent,
        )
        Spacer(Modifier.size(4.dp))
        ToggleRow(
            label = "Sound",
            description = "Play DTMF tones on key press.",
            checked = settings.sound,
            onCheckedChange = viewModel::setSound,
            accent = accent,
        )

        // Precision slider.
        Spacer(Modifier.size(16.dp))
        SectionLabel("Math precision")
        PrecisionRow(
            precision = settings.precision,
            onChange = viewModel::setPrecision,
            accent = accent,
        )

        // Privacy.
        Spacer(Modifier.size(16.dp))
        SectionLabel("Privacy")
        ToggleRow(
            label = "Crash reporting",
            description = "Anonymous crash reports. Off by default. " +
                "Nothing leaves the device until you opt in.",
            checked = settings.crashOptIn,
            onCheckedChange = viewModel::setCrashOptIn,
            accent = accent,
        )

        // About.
        Spacer(Modifier.size(16.dp))
        SectionLabel("About")
        AboutRow(label = "Version", value = "1.0.0-dev")
        AboutRow(label = "License", value = "Apache 2.0")
        AboutRow(label = "GitHub", value = "zeevy/android-calculator")

        Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
    )
}

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
                    uncheckedTrackColor = Color(0xFF555555),
                    uncheckedBorderColor = Color.Transparent,
                ),
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
                text = option.label(),
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
                text = "Significant figures",
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
                DataStoreSettingsRepository.MIN_PRECISION.toFloat()..
                    DataStoreSettingsRepository.MAX_PRECISION.toFloat(),
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
            text =
                "Trades precision for speed - at 6 digits a result like " +
                    "1/3 reads as 0.333333; at 16 it reads " +
                    "0.3333333333333333. Engine default is 12.",
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

private fun UserSettings.ThemeOption.label(): String =
    when (this) {
        UserSettings.ThemeOption.System -> "System"
        UserSettings.ThemeOption.Light -> "Light"
        UserSettings.ThemeOption.Dark -> "Dark"
    }

// Background tint for settings rows. Slightly lighter than the
// sheet container so individual rows read as distinct cards.
private val RowBackground = Color(0xFF2C2C2E)

// Same orange as the keypad operator keys. Reused here so the settings
// sheet stays visually consistent with the rest of the app.
private val SettingsAccent = Color(0xFFFF9F0A)

@Suppress("unused")
private val PaddingPlaceholder = PaddingValues(0.dp)
