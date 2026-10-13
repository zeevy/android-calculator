package com.calculator.feature.converter.base

import com.calculator.R
import com.calculator.core.common.input.InputResult
import com.calculator.core.domain.math.BaseConverter.BIN
import com.calculator.core.domain.math.BaseConverter.DEC
import com.calculator.core.domain.math.BaseConverter.HEX
import com.calculator.core.domain.math.BaseConverter.OCT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** The typed base field, to a value or an error, and what each field shows. */
class BaseConverterInputTest {
    @Test
    fun `valid text in any base gives the value`() {
        assertEquals(InputResult.Ok(42L), baseConverterInput("42", DEC))
        assertEquals(InputResult.Ok(42L), baseConverterInput("2a", HEX))
        assertEquals(InputResult.Ok(172L), baseConverterInput("1010_1100", BIN))
        assertEquals(InputResult.Ok(1L), baseConverterInput("1_", OCT))
    }

    @Test
    fun `empty text asks for a value in that field`() {
        assertEquals(InputResult.Invalid(R.string.basecalc_dec, R.string.input_error_required), baseConverterInput("", DEC))
        assertEquals(InputResult.Invalid(R.string.basecalc_bin, R.string.input_error_required), baseConverterInput("_", BIN))
    }

    @Test
    fun `bad digits and too many digits name the field`() {
        assertEquals(InputResult.Invalid(R.string.basecalc_hex, R.string.input_error_invalid), baseConverterInput("1G", HEX))
        assertEquals(
            InputResult.Invalid(R.string.basecalc_dec, R.string.input_error_too_large),
            baseConverterInput("9223372036854775808", DEC),
        )
    }

    @Test
    fun `the typed field keeps its text and the others follow the value`() {
        assertEquals("0ff", baseFieldText(HEX, sourceRadix = HEX, sourceText = "0ff", lastValue = 255))
        assertEquals("255", baseFieldText(DEC, sourceRadix = HEX, sourceText = "0ff", lastValue = 255))
        assertEquals("FF", baseFieldText(HEX, sourceRadix = DEC, sourceText = "255", lastValue = 255))
        assertEquals("11111111", baseFieldText(BIN, sourceRadix = DEC, sourceText = "255", lastValue = 255))
    }
}
