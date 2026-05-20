package com.calculator.baselineprofile

import android.os.Build
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assume.assumeFalse
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
        // FrameTimingMetric reads SurfaceFlinger frame traces. The stripped
        // aosp_atd emulator image used in CI does not produce them, so the
        // metric returns zero samples and the test crashes. Skip on
        // emulators; real-device runs (the only environment where frame
        // timings are meaningful anyway) still execute.
        assumeFalse(
            "Skipped: FrameTimingMetric needs real rendering, emulator returns 0 samples",
            isEmulator(),
        )
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

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.PRODUCT.contains("sdk_") ||
            Build.PRODUCT.contains("emulator") ||
            Build.PRODUCT.contains("simulator")

    companion object {
        private val TARGET_PACKAGE: String by lazy {
            InstrumentationRegistry.getArguments().getString(
                "targetAppId",
            ) ?: "com.calculator"
        }
    }
}
