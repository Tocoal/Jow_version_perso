package com.perso.jow.core.shopping

import com.perso.jow.core.unit.MeasureUnit
import com.perso.jow.core.unit.QuantityFormatter
import com.perso.jow.core.unit.UnitCategory
import com.perso.jow.core.unit.UnitConverter
import com.perso.jow.core.unit.UnitFormatter

/**
 * One recipe's ingredient line, already scaled by the servings multiplier chosen
 * for that recipe in the current shopping session.
 */
data class AggregationInput(
    val ingredientKey: String,
    val ingredientName: String,
    val quantity: Double,
    val unit: MeasureUnit
)

data class AggregatedQuantity(
    val ingredientKey: String,
    val ingredientName: String,
    val quantity: Double,
    val unit: MeasureUnit
)

/**
 * Combines ingredient lines coming from several recipes into a single shopping list:
 * quantities for the same ingredient are summed, converting between compatible units
 * (grams/kilograms, milliliters/centiliters/liters) so "300g" + "0.2kg" becomes "500g".
 * Units that aren't reliably convertible (tablespoon, pinch, piece...) are only summed
 * when they already match exactly, and otherwise kept as separate lines.
 */
object ShoppingListAggregator {

    fun aggregate(inputs: List<AggregationInput>): List<AggregatedQuantity> {
        data class ConvertibleKey(val ingredientKey: String, val category: UnitCategory)
        data class DirectKey(val ingredientKey: String, val unit: MeasureUnit)

        val convertibleGroups = LinkedHashMap<ConvertibleKey, MutableList<AggregationInput>>()
        val directGroups = LinkedHashMap<DirectKey, MutableList<AggregationInput>>()

        for (input in inputs) {
            if (input.quantity == 0.0) continue
            if (input.unit.category == UnitCategory.MASS || input.unit.category == UnitCategory.VOLUME) {
                convertibleGroups.getOrPut(ConvertibleKey(input.ingredientKey, input.unit.category)) { mutableListOf() }.add(input)
            } else {
                directGroups.getOrPut(DirectKey(input.ingredientKey, input.unit)) { mutableListOf() }.add(input)
            }
        }

        val result = mutableListOf<AggregatedQuantity>()

        convertibleGroups.forEach { (key, items) ->
            val baseSum = items.sumOf { UnitConverter.toBase(it.quantity, it.unit) }
            val (quantity, unit) = when (key.category) {
                UnitCategory.MASS -> UnitFormatter.bestMassUnit(baseSum)
                UnitCategory.VOLUME -> UnitFormatter.bestVolumeUnit(baseSum)
                else -> QuantityFormatter.round(baseSum) to items.first().unit
            }
            result += AggregatedQuantity(key.ingredientKey, items.first().ingredientName, quantity, unit)
        }

        directGroups.forEach { (key, items) ->
            val sum = QuantityFormatter.round(items.sumOf { it.quantity })
            result += AggregatedQuantity(key.ingredientKey, items.first().ingredientName, sum, key.unit)
        }

        return result.sortedBy { it.ingredientName.lowercase() }
    }
}
