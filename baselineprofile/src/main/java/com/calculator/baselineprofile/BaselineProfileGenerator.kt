package com.calculator.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Walks the calculator through its most-trodden paths so the
 * [BaselineProfileRule] can record which classes / methods get JIT'd.
 * The resulting profile is written to
 * `app/src/main/baseline-prof.txt` automatically by the
 * `androidx.baselineprofile` plugin.
 *
 * Cold-start path:
 *  - Launch the app.
 *  - Type a basic expression and press equals.
 *
 * Other useful paths (commented out by default to keep the profile
 * generation under a minute; uncomment when adding new heavy screens):
 *  - Open the tools sheet and switch to Advanced mode.
 *  - Type a scientific expression and evaluate.
 *  - Open the Unit Converter and pick a unit.
 */
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = TARGET_PACKAGE) {
            startActivityAndWait()

            // Wait for the calculator to settle. The "0" placeholder
            // shows up on the display once Compose draws the first
            // frame; that's a reliable "we're interactive now" signal.
            device.wait(Until.hasObject(By.text("0")), 5_000)

            // Tap a few digits + an operator + equals so the hot
            // expression-evaluation path gets profiled.
            tapByText("1")
            tapByText("+")
            tapByText("2")
            tapByText("=")
            device.wait(Until.hasObject(By.text("3")), 2_000)
        }
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.tapByText(text: String) {
        device.findObject(By.text(text))?.click()
        device.waitForIdle()
    }

    companion object {
        private val TARGET_PACKAGE: String by lazy {
            InstrumentationRegistry.getArguments().getString(
                "targetAppId",
            ) ?: "com.calculator"
        }
    }
}
