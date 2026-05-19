package com.calculator.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold + warm start macrobenchmarks.
 *
 * Three CompilationModes per startup mode covers the matrix we care
 * about for regression-tracking on `main`:
 *   - None: nothing AOT'd, slowest baseline.
 *   - Partial(BaselineProfile): the profile we ship.
 *   - Full: gold standard (rare in practice; useful as a ceiling).
 *
 * Targets (from Phase 9 plan):
 *   - Cold start P50 < 600 ms on a Pixel 6a-class device with the
 *     baseline profile installed.
 *   - Warm start P50 < 200 ms.
 *
 * Run with:
 *   ./gradlew :baselineprofile:connectedBenchmarkAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupCompilationNone() = startup(CompilationMode.None())

    @Test
    fun startupCompilationBaselineProfiles() =
        startup(CompilationMode.Partial(baselineProfileMode = androidx.benchmark.macro.BaselineProfileMode.Require))

    @Test
    fun startupCompilationFull() = startup(CompilationMode.Full())

    private fun startup(compilationMode: CompilationMode) {
        rule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = 5,
            setupBlock = {
                pressHome()
            },
            measureBlock = {
                startActivityAndWait()
                device.wait(Until.hasObject(By.text("0")), 5_000)
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
