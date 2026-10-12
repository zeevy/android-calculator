package com.calculator.feature.floating

import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.calculator.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowWindowManagerImpl
import kotlin.test.assertEquals
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

    @Test
    fun overlayKeypadFollowsOperatorPrecedence() {
        ShadowSettings.setCanDrawOverlays(true)
        val service = Robolectric.buildService(FloatingCalculatorService::class.java).create().get()
        val windows = Shadow.extract<ShadowWindowManagerImpl>(service.getSystemService(WindowManager::class.java))
        val root = windows.views.single()

        listOf(R.id.floating_2, R.id.floating_add, R.id.floating_3, R.id.floating_mul, R.id.floating_4, R.id.floating_eq)
            .forEach { root.findViewById<View>(it).performClick() }

        assertEquals("14", root.findViewById<TextView>(R.id.floating_display).text.toString())
    }
}
