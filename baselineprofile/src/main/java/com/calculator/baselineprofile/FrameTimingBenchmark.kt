package com.calculator.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame-timing benchmark covering the keypad tap path - the most
 * common user interaction, and the one most likely to drop frames
 * because every tap recomposes the display + plays a tone +
 * fires haptics.
 *
 * Target: no P95 frame over the device's vsync budget (~16.67 ms on a
 * 60 Hz display, ~8.33 ms on a 120 Hz display).
 */
@RunWith(AndroidJUnit4::class)
class FrameTimingBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun keypadTapping() {
        rule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode =
                CompilationMode.Partial(
                    baselineProfileMode = androidx.benchmark.macro.BaselineProfileMode.Require,
                ),
            startupMode = StartupMode.COLD,
            iterations = 5,
            setupBlock = {
                startActivityAndWait()
                device.wait(Until.hasObject(By.text("0")), 5_000)
            },
            measureBlock = {
                // 12 taps + an equals = enough recompositions to surface
                // a slow frame if one exists.
                listOf("1", "+", "2", "+", "3", "+", "4", "+", "5", "+", "6", "=")
                    .forEach { label ->
                        device.findObject(By.text(label))?.click()
                        device.waitForIdle()
                    }
            },
        )
    }

    companion object {
        private val TARGET_PACKAGE: String by lazy {
            InstrumentationRegistry.getArguments().getString(
                "targetAppId",
            ) ?: "com.calculator"
        }
    }
}
