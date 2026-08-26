package com.perso.jow.app.data.repository

import androidx.room.withTransaction
import com.perso.jow.app.data.db.AppDatabase
import com.perso.jow.app.data.db.HistoryEntryEntity
import com.perso.jow.app.data.db.ShoppingListItemDetail
import com.perso.jow.app.data.db.ShoppingListItemEntity
import com.perso.jow.app.data.db.ShoppingSessionEntity
import com.perso.jow.app.data.db.ShoppingSessionRecipeDetail
import com.perso.jow.app.data.db.ShoppingSessionRecipeEntity
import com.perso.jow.app.data.db.ShoppingSessionStatus
import com.perso.jow.core.shopping.AggregationInput
import com.perso.jow.core.shopping.ShoppingListAggregator
import com.perso.jow.core.unit.MeasureUnit
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(private val db: AppDatabase) {
    private val shoppingDao = db.shoppingDao()
    private val recipeDao = db.recipeDao()
    private val historyDao = db.historyDao()

    fun observeShoppingSession(): Flow<ShoppingSessionEntity?> =
        shoppingDao.observeLatestSessionByStatus(ShoppingSessionStatus.SHOPPING)

    fun observeCookingSession(): Flow<ShoppingSessionEntity?> =
        shoppingDao.observeLatestSessionByStatus(ShoppingSessionStatus.COOKING)

    fun observeSessionRecipes(sessionId: Long): Flow<List<ShoppingSessionRecipeDetail>> =
        shoppingDao.observeSessionRecipes(sessionId)

    fun observeListItems(sessionId: Long): Flow<List<ShoppingListItemDetail>> =
        shoppingDao.observeListItems(sessionId)

    suspend fun getOrCreateShoppingSession(): ShoppingSessionEntity {
        shoppingDao.getLatestSessionByStatus(ShoppingSessionStatus.SHOPPING)?.let { return it }
        val id = shoppingDao.insertSession(ShoppingSessionEntity(status = ShoppingSessionStatus.SHOPPING))
        return shoppingDao.getSession(id)!!
    }

    /**
     * Replaces the recipes picked for [sessionId] (each with its own servings multiplier)
     * and regenerates the aggregated shopping list, keeping check marks for ingredient/unit
     * pairs that are still present.
     */
    suspend fun setSessionRecipes(sessionId: Long, selections: List<Pair<Long, Double>>) = db.withTransaction {
        val previousChecks = shoppingDao.getListItems(sessionId)
            .associate { (it.ingredientId to it.unit) to it.isChecked }

        shoppingDao.clearSessionRecipes(sessionId)
        if (selections.isNotEmpty()) {
            shoppingDao.insertSessionRecipes(
                selections.map { (recipeId, multiplier) ->
                    ShoppingSessionRecipeEntity(sessionId = sessionId, recipeId = recipeId, servingsMultiplier = multiplier)
                }
            )
        }

        val ingredientLines = selections.flatMap { (recipeId, multiplier) ->
            recipeDao.getIngredientsForRecipe(recipeId).map { detail ->
                AggregationInput(
                    ingredientKey = detail.line.ingredientId.toString(),
                    ingredientName = detail.ingredient.name,
                    quantity = detail.line.quantity * multiplier,
                    unit = MeasureUnit.fromLabel(detail.line.unit)
                )
            }
        }
        val aggregated = ShoppingListAggregator.aggregate(ingredientLines)

        shoppingDao.clearListItems(sessionId)
        shoppingDao.insertListItems(
            aggregated.map {
                val ingredientId = it.ingredientKey.toLong()
                ShoppingListItemEntity(
                    sessionId = sessionId,
                    ingredientId = ingredientId,
                    quantity = it.quantity,
                    unit = it.unit.label,
                    isChecked = previousChecks[ingredientId to it.unit.label] ?: false
                )
            }
        )
    }

    suspend fun setItemChecked(item: ShoppingListItemEntity, isChecked: Boolean) =
        shoppingDao.updateListItem(item.copy(isChecked = isChecked))

    /** Shopping is done: the session's recipes move from "à acheter" to "en cours de cuisine". */
    suspend fun finishShopping(sessionId: Long) {
        val session = shoppingDao.getSession(sessionId) ?: return
        shoppingDao.updateSession(session.copy(status = ShoppingSessionStatus.COOKING))
    }

    /** Marks one recipe of a cooking session as cooked, logs it to history, and closes the session once every recipe is done. */
    suspend fun markRecipeCooked(
        sessionId: Long,
        sessionRecipe: ShoppingSessionRecipeEntity,
        recipeName: String,
        servings: Int
    ) = db.withTransaction {
        shoppingDao.updateSessionRecipe(sessionRecipe.copy(isCooked = true))
        historyDao.insert(HistoryEntryEntity(recipeId = sessionRecipe.recipeId, recipeName = recipeName, servings = servings))

        val remaining = shoppingDao.getSessionRecipes(sessionId)
        if (remaining.all { it.isCooked }) {
            shoppingDao.getSession(sessionId)?.let {
                shoppingDao.updateSession(it.copy(status = ShoppingSessionStatus.DONE))
            }
        }
    }
}
