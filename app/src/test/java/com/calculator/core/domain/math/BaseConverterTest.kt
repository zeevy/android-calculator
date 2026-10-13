package com.calculator.core.domain.math

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Parsing and formatting across the four bases in [BaseConverter]. */
class BaseConverterTest {
    private val radixes = listOf(BaseConverter.BIN, BaseConverter.OCT, BaseConverter.DEC, BaseConverter.HEX)

    @Test
    fun `one value reads the same in every base`() {
        assertEquals(255L, BaseConverter.parse("11111111", BaseConverter.BIN))
        assertEquals(255L, BaseConverter.parse("377", BaseConverter.OCT))
        assertEquals(255L, BaseConverter.parse("255", BaseConverter.DEC))
        assertEquals(255L, BaseConverter.parse("FF", BaseConverter.HEX))
    }

    @Test
    fun `format then parse gives back the same value in every base`() {
        val values = listOf(0L, 1L, 7L, 8L, 10L, 255L, 4096L, 123_456_789L, Long.MAX_VALUE)
        for (radix in radixes) {
            for (value in values) {
                assertEquals(value, BaseConverter.parse(BaseConverter.format(value, radix), radix), "radix $radix value $value")
            }
        }
    }

    @Test
    fun `hex output is upper case and other bases are plain digits`() {
        assertEquals("DEADBEEF", BaseConverter.format(0xDEADBEEFL, BaseConverter.HEX))
        assertEquals("11111111", BaseConverter.format(255L, BaseConverter.BIN))
        assertEquals("377", BaseConverter.format(255L, BaseConverter.OCT))
        assertEquals("255", BaseConverter.format(255L, BaseConverter.DEC))
    }

    @Test
    fun `hex input takes lower and upper case`() {
        assertEquals(0xABCL, BaseConverter.parse("abc", BaseConverter.HEX))
        assertEquals(0xABCL, BaseConverter.parse("ABC", BaseConverter.HEX))
    }

    @Test
    fun `underscores and outer spaces are ignored`() {
        assertEquals(0b1010_1100L, BaseConverter.parse("1010_1100", BaseConverter.BIN))
        assertEquals(1_000_000L, BaseConverter.parse(" 1_000_000 ", BaseConverter.DEC))
    }

    @Test
    fun `digits outside the base give null`() {
        assertNull(BaseConverter.parse("102", BaseConverter.BIN))
        assertNull(BaseConverter.parse("78", BaseConverter.OCT))
        assertNull(BaseConverter.parse("12A", BaseConverter.DEC))
        assertNull(BaseConverter.parse("FG", BaseConverter.HEX))
    }

    @Test
    fun `empty or blank text gives null`() {
        assertNull(BaseConverter.parse("", BaseConverter.DEC))
        assertNull(BaseConverter.parse("   ", BaseConverter.HEX))
        assertNull(BaseConverter.parse("__", BaseConverter.BIN))
    }
}
