package com.calculator.feature.converter.unit

import com.calculator.core.domain.converter.ConversionTable
import com.calculator.core.domain.converter.UnitCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Every converter unit and category has a translated name. */
class UnitNamesTest {
    @Test
    fun `every unit in every category maps to a string`() {
        val units = UnitCategory.entries.flatMap { ConversionTable.unitsFor(it) }
        val names = units.map { it.labelRes() }
        assertEquals(units.size, names.toSet().size)
        assertEquals(
            UnitCategory.entries.size,
            UnitCategory.entries
                .map { it.labelRes() }
                .toSet()
                .size,
        )
    }
}
