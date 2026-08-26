package com.perso.jow.core.shopping

import com.perso.jow.core.unit.MeasureUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShoppingListAggregatorTest {

    @Test
    fun `sums same ingredient and same unit across recipes`() {
        val result = ShoppingListAggregator.aggregate(
            listOf(
                AggregationInput("tomato", "Tomate", 2.0, MeasureUnit.PIECE),
                AggregationInput("tomato", "Tomate", 3.0, MeasureUnit.PIECE)
            )
        )
        assertEquals(1, result.size)
        assertEquals(5.0, result[0].quantity)
        assertEquals(MeasureUnit.PIECE, result[0].unit)
    }

    @Test
    fun `converts and sums compatible mass units`() {
        val result = ShoppingListAggregator.aggregate(
            listOf(
                AggregationInput("flour", "Farine", 300.0, MeasureUnit.GRAM),
                AggregationInput("flour", "Farine", 0.2, MeasureUnit.KILOGRAM)
            )
        )
        assertEquals(1, result.size)
        assertEquals(500.0, result[0].quantity)
        assertEquals(MeasureUnit.GRAM, result[0].unit)
    }

    @Test
    fun `converts and sums compatible volume units switching to liters`() {
        val result = ShoppingListAggregator.aggregate(
            listOf(
                AggregationInput("milk", "Lait", 700.0, MeasureUnit.MILLILITER),
                AggregationInput("milk", "Lait", 50.0, MeasureUnit.CENTILITER)
            )
        )
        assertEquals(1, result.size)
        assertEquals(1.2, result[0].quantity)
        assertEquals(MeasureUnit.LITER, result[0].unit)
    }

    @Test
    fun `keeps non convertible units of the same ingredient as separate lines`() {
        val result = ShoppingListAggregator.aggregate(
            listOf(
                AggregationInput("salt", "Sel", 1.0, MeasureUnit.TABLESPOON),
                AggregationInput("salt", "Sel", 2.0, MeasureUnit.PINCH)
            )
        )
        assertEquals(2, result.size)
        assertEquals(setOf(MeasureUnit.TABLESPOON, MeasureUnit.PINCH), result.map { it.unit }.toSet())
    }

    @Test
    fun `keeps different ingredients separate and sorts by name`() {
        val result = ShoppingListAggregator.aggregate(
            listOf(
                AggregationInput("egg", "Oeuf", 6.0, MeasureUnit.PIECE),
                AggregationInput("apple", "Pomme", 4.0, MeasureUnit.PIECE)
            )
        )
        assertEquals(listOf("Oeuf", "Pomme"), result.map { it.ingredientName })
    }

    @Test
    fun `ignores zero quantity lines`() {
        val result = ShoppingListAggregator.aggregate(
            listOf(AggregationInput("salt", "Sel", 0.0, MeasureUnit.GRAM))
        )
        assertEquals(0, result.size)
    }
}
