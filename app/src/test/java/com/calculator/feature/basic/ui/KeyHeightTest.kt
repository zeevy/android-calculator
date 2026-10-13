package com.calculator.feature.basic.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Keys keep a 48dp minimum unless that would push the display off screen. */
class KeyHeightTest {
    @Test
    fun `a normal phone gets 48dp keys in both modes`() {
        assertEquals(48f, minKeyHeightDp(screenWidthDp = 411, screenHeightDp = 914, scientific = true))
        assertEquals(48f, minKeyHeightDp(screenWidthDp = 411, screenHeightDp = 914, scientific = false))
    }

    @Test
    fun `the largest display size keeps the display in scientific mode`() {
        assertEquals(0f, minKeyHeightDp(screenWidthDp = 288, screenHeightDp = 640, scientific = true))
        assertEquals(48f, minKeyHeightDp(screenWidthDp = 288, screenHeightDp = 640, scientific = false))
    }
}
