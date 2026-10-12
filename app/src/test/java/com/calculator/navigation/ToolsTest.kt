package com.calculator.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.util.Locale

/** The tool list that drives the tools menu and the launcher shortcuts. */
class ToolsTest {
    private val us = Locale.US
    private val india = Locale.forLanguageTag("en-IN")
    private val newYork = ZoneId.of("America/New_York")
    private val kolkata = ZoneId.of("Asia/Kolkata")

    @Test
    fun `GST is hidden for a US locale outside India`() {
        assertFalse(availableTools(us, newYork).any { it.route == GstRoute })
        assertNull(availableToolById("gst", us, newYork))
    }

    @Test
    fun `GST is shown for an Indian locale`() {
        assertTrue(availableTools(india, newYork).any { it.route == GstRoute })
        assertNotNull(availableToolById("gst", india, newYork))
    }

    @Test
    fun `GST is shown for an English US phone set to India time`() {
        assertTrue(availableTools(us, kolkata).any { it.route == GstRoute })
        assertTrue(isIndia(us, ZoneId.of("Asia/Calcutta")))
    }

    @Test
    fun `every other tool is offered everywhere`() {
        assertEquals(Tools.size - 1, availableTools(us, newYork).size)
    }

    @Test
    fun `ids and routes are unique`() {
        assertEquals(Tools.size, Tools.map { it.id }.toSet().size)
        assertEquals(Tools.size, Tools.map { it.route }.toSet().size)
    }
}
