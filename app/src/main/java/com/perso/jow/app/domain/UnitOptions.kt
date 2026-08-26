package com.perso.jow.app.domain

import com.perso.jow.core.unit.MeasureUnit

/** French display label for each storage unit code (MeasureUnit.label stays a stable, code-like string). */
data class UnitOption(val unit: MeasureUnit, val displayLabel: String)

val UNIT_OPTIONS = listOf(
    UnitOption(MeasureUnit.GRAM, "g"),
    UnitOption(MeasureUnit.KILOGRAM, "kg"),
    UnitOption(MeasureUnit.MILLILITER, "ml"),
    UnitOption(MeasureUnit.CENTILITER, "cl"),
    UnitOption(MeasureUnit.LITER, "l"),
    UnitOption(MeasureUnit.PIECE, "pièce(s)"),
    UnitOption(MeasureUnit.TABLESPOON, "c. à soupe"),
    UnitOption(MeasureUnit.TEASPOON, "c. à café"),
    UnitOption(MeasureUnit.PINCH, "pincée")
)

fun MeasureUnit.displayLabel(): String = UNIT_OPTIONS.find { it.unit == this }?.displayLabel ?: label

fun formatQuantity(quantity: Double, unit: MeasureUnit): String {
    val trimmed = if (quantity == quantity.toLong().toDouble()) quantity.toLong().toString() else quantity.toString()
    return "$trimmed ${unit.displayLabel()}"
}
