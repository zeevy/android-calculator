package com.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
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
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate. The platform splash will
        // remain visible until the first Compose frame draws.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Launcher shortcuts pass an extra here so the nav host can
        // jump straight to that destination instead of starting on the
        // basic calculator. Reading once at onCreate is enough because
        // shortcuts always relaunch the task (singleTask in the manifest).
        val shortcut = intent?.getStringExtra(SHORTCUT_DESTINATION)

        setContent {
            ThemedCalculatorRoot(startDestinationHint = shortcut)
        }
    }

    companion object {
        const val SHORTCUT_DESTINATION = "com.calculator.SHORTCUT_DESTINATION"
    }
}

/**
 * Reads [SettingsViewModel] for the persisted theme + dynamicColor
 * choices, then wraps the nav host in a configured [CalculatorTheme].
 * Lives inside the Activity so hiltViewModel() resolves against the
 * ComponentActivity's ViewModelStoreOwner.
 */
@Composable
private fun ThemedCalculatorRoot(
    startDestinationHint: String? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
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
