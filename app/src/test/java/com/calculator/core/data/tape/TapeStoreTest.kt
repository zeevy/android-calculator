package com.calculator.core.data.tape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The session tape keeps the newest 200 lines and supports remove and clear. */
class TapeStoreTest {
    private val store = TapeStore()

    @Test
    fun `keeps only the newest 200 lines, oldest first`() {
        repeat(205) { store.add("$it+0", "$it") }

        val lines = store.entries.value
        assertEquals(200, lines.size)
        assertEquals("5", lines.first().result)
        assertEquals("204", lines.last().result)
    }

    @Test
    fun `remove drops one line and clear drops all`() {
        store.add("1+1", "2")
        store.add("2+2", "4")
        store.remove(
            store.entries.value
                .first()
                .id,
        )
        assertEquals(listOf("4"), store.entries.value.map { it.result })

        store.clear()
        assertTrue(store.entries.value.isEmpty())
    }
}
