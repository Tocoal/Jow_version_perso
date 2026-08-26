package com.perso.jow.core.unit

/**
 * A unit family that quantities can be summed/converted within.
 * MASS and VOLUME support conversion between their units; COUNT and OTHER
 * units (tablespoon, pinch, piece...) can only be summed with themselves,
 * since they don't convert reliably to a common base without ingredient density.
 */
enum class UnitCategory {
    MASS,
    VOLUME,
    COUNT,
    OTHER
}

/**
 * [toBaseFactor] expresses how many base units (gram for MASS, milliliter for VOLUME)
 * one unit of this enum represents. Ignored for COUNT/OTHER, which never convert.
 */
enum class MeasureUnit(val label: String, val category: UnitCategory, val toBaseFactor: Double) {
    GRAM("g", UnitCategory.MASS, 1.0),
    KILOGRAM("kg", UnitCategory.MASS, 1000.0),
    MILLILITER("ml", UnitCategory.VOLUME, 1.0),
    CENTILITER("cl", UnitCategory.VOLUME, 10.0),
    LITER("l", UnitCategory.VOLUME, 1000.0),
    PIECE("piece", UnitCategory.COUNT, 1.0),
    TABLESPOON("tbsp", UnitCategory.OTHER, 1.0),
    TEASPOON("tsp", UnitCategory.OTHER, 1.0),
    PINCH("pinch", UnitCategory.OTHER, 1.0);

    companion object {
        fun fromLabelOrNull(label: String): MeasureUnit? = entries.find { it.label.equals(label, ignoreCase = true) }
        fun fromLabel(label: String): MeasureUnit = fromLabelOrNull(label) ?: PIECE
    }
}
