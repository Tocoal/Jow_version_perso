package com.perso.jow.core.unit

import kotlin.math.round as mathRound

object UnitConverter {

    fun canConvert(from: MeasureUnit, to: MeasureUnit): Boolean =
        from == to || (from.category == to.category && (from.category == UnitCategory.MASS || from.category == UnitCategory.VOLUME))

    fun toBase(quantity: Double, unit: MeasureUnit): Double = quantity * unit.toBaseFactor

    fun fromBase(baseQuantity: Double, unit: MeasureUnit): Double = baseQuantity / unit.toBaseFactor

    /** Returns null when the two units belong to non-convertible families (e.g. grams vs. tablespoons). */
    fun convert(quantity: Double, from: MeasureUnit, to: MeasureUnit): Double? {
        if (!canConvert(from, to)) return null
        return fromBase(toBase(quantity, from), to)
    }
}

object QuantityFormatter {
    /** Rounds to two decimal places, which is plenty of precision for a shopping list. */
    fun round(value: Double): Double = mathRound(value * 100.0) / 100.0
}

/** Picks a human-friendly unit for an aggregated base quantity, e.g. 1500g -> 1.5kg. */
object UnitFormatter {
    fun bestMassUnit(grams: Double): Pair<Double, MeasureUnit> =
        if (grams >= 1000.0) QuantityFormatter.round(grams / 1000.0) to MeasureUnit.KILOGRAM
        else QuantityFormatter.round(grams) to MeasureUnit.GRAM

    fun bestVolumeUnit(milliliters: Double): Pair<Double, MeasureUnit> =
        if (milliliters >= 1000.0) QuantityFormatter.round(milliliters / 1000.0) to MeasureUnit.LITER
        else QuantityFormatter.round(milliliters) to MeasureUnit.MILLILITER
}
