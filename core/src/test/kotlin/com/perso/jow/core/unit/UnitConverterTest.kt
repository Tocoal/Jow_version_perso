package com.perso.jow.core.unit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UnitConverterTest {

    @Test
    fun `converts grams to kilograms`() {
        assertEquals(1.5, UnitConverter.convert(1500.0, MeasureUnit.GRAM, MeasureUnit.KILOGRAM))
    }

    @Test
    fun `converts liters to milliliters`() {
        assertEquals(250.0, UnitConverter.convert(0.25, MeasureUnit.LITER, MeasureUnit.MILLILITER))
    }

    @Test
    fun `same unit converts to itself unchanged`() {
        assertEquals(42.0, UnitConverter.convert(42.0, MeasureUnit.PIECE, MeasureUnit.PIECE))
    }

    @Test
    fun `mass cannot convert to volume`() {
        assertNull(UnitConverter.convert(100.0, MeasureUnit.GRAM, MeasureUnit.MILLILITER))
    }

    @Test
    fun `tablespoon cannot convert to teaspoon`() {
        assertNull(UnitConverter.convert(1.0, MeasureUnit.TABLESPOON, MeasureUnit.TEASPOON))
    }

    @Test
    fun `best mass unit switches to kilograms at 1000g`() {
        val (quantity, unit) = UnitFormatter.bestMassUnit(1500.0)
        assertEquals(1.5, quantity)
        assertEquals(MeasureUnit.KILOGRAM, unit)
    }

    @Test
    fun `best mass unit stays in grams below 1000g`() {
        val (quantity, unit) = UnitFormatter.bestMassUnit(250.0)
        assertEquals(250.0, quantity)
        assertEquals(MeasureUnit.GRAM, unit)
    }
}
