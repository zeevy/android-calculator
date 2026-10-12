package com.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calculator.core.data.settings.UserSettings
import com.calculator.core.designsystem.theme.CalculatorTheme
import com.calculator.feature.settings.SettingsViewModel
import com.calculator.navigation.CalculatorNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity hosting the entire Compose UI tree.
 *
 * Responsibilities are intentionally limited:
 *  1. Install the SplashScreen API before `super.onCreate` so the system
 *     splash transitions cleanly into the Compose content.
 *  2. Enable edge-to-edge so the app draws behind the status and
 *     navigation bars (the design system applies the appropriate
 *     window-insets handling per screen).
 *  3. Wire the root [CalculatorNavHost] inside the [CalculatorTheme].
 *
 * All UI state lives in Composables and ViewModels; this Activity holds
 * no business logic.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate. The splash stays up until
        // the saved settings are read, so the first frame already uses
        // the saved theme (no light flash for a user who chose Dark).
        installSplashScreen().setKeepOnScreenCondition { settingsViewModel.settings.value == null }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Launcher shortcuts pass an extra here so the nav host can
        // jump straight to that destination instead of starting on the
        // basic calculator. Reading once at onCreate is enough because
        // shortcuts always relaunch the task (singleTask in the manifest).
        val shortcut = intent?.getStringExtra(SHORTCUT_DESTINATION)

        setContent {
            ThemedCalculatorRoot(startDestinationHint = shortcut, viewModel = settingsViewModel)
        }
    }

    companion object {
        const val SHORTCUT_DESTINATION = "com.calculator.SHORTCUT_DESTINATION"
    }
}

/**
 * Reads [SettingsViewModel] for the persisted theme + dynamicColor
 * choices, then wraps the nav host in a configured [CalculatorTheme].
 */
@Composable
private fun ThemedCalculatorRoot(
    startDestinationHint: String?,
    viewModel: SettingsViewModel,
) {
    // Nothing is drawn until the settings load; the splash covers it.
    val settings = viewModel.settings.collectAsStateWithLifecycle().value ?: return
    val darkTheme =
        when (settings.theme) {
            UserSettings.ThemeOption.System -> isSystemInDarkTheme()
            UserSettings.ThemeOption.Light -> false
            UserSettings.ThemeOption.Dark -> true
        }
    CalculatorTheme(
        darkTheme = darkTheme,
        dynamicColor = settings.dynamicColor,
    ) {
        CalculatorNavHost(startDestinationHint = startDestinationHint)
    }
}
