package com.calculator.feature.floating

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Start-up guards of [FloatingCalculatorService]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FloatingCalculatorServiceTest {
    @Test
    fun stopsItselfWithoutCrashingWhenOverlayPermissionIsMissing() {
        ShadowSettings.setCanDrawOverlays(false)

        val service = Robolectric.buildService(FloatingCalculatorService::class.java).create().get()

        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    @Test
    fun keepsRunningWhenOverlayPermissionIsGranted() {
        ShadowSettings.setCanDrawOverlays(true)

        val service = Robolectric.buildService(FloatingCalculatorService::class.java).create().get()

        assertFalse(shadowOf(service).isStoppedBySelf)
    }
}
