package com.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.calculator.core.designsystem.theme.CalculatorTheme
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

        setContent {
            CalculatorTheme {
                CalculatorNavHost()
            }
        }
    }
}
