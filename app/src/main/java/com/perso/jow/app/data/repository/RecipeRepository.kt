package com.perso.jow.app.data.repository

import androidx.room.withTransaction
import com.perso.jow.app.data.db.AppDatabase
import com.perso.jow.app.data.db.IngredientEntity
import com.perso.jow.app.data.db.RecipeEntity
import com.perso.jow.app.data.db.RecipeIngredientDetail
import com.perso.jow.app.data.db.RecipeIngredientEntity
import kotlinx.coroutines.flow.Flow

data class RecipeIngredientInput(
    val ingredientName: String,
    val quantity: Double,
    val unit: String
)

class RecipeRepository(private val db: AppDatabase) {
    private val recipeDao = db.recipeDao()
    private val ingredientDao = db.ingredientDao()

    fun observeRecipes(): Flow<List<RecipeEntity>> = recipeDao.observeAll()

    fun observeFavorites(): Flow<List<RecipeEntity>> = recipeDao.observeFavorites()

    fun observeRecipe(id: Long): Flow<RecipeEntity?> = recipeDao.observeById(id)

    fun observeIngredients(recipeId: Long): Flow<List<RecipeIngredientDetail>> = recipeDao.observeIngredientsForRecipe(recipeId)

    suspend fun getIngredients(recipeId: Long): List<RecipeIngredientDetail> = recipeDao.getIngredientsForRecipe(recipeId)

    suspend fun setFavorite(id: Long, isFavorite: Boolean) = recipeDao.setFavorite(id, isFavorite)

    suspend fun deleteRecipe(recipe: RecipeEntity) = recipeDao.delete(recipe)

    /** Creates or updates a recipe and replaces its ingredient lines, reusing/creating ingredients by name. */
    suspend fun saveRecipe(recipe: RecipeEntity, ingredients: List<RecipeIngredientInput>): Long = db.withTransaction {
        val recipeId = if (recipe.id == 0L) {
            recipeDao.insert(recipe)
        } else {
            recipeDao.update(recipe)
            recipe.id
        }

        recipeDao.clearIngredientsForRecipe(recipeId)
        val rows = ingredients.mapIndexedNotNull { index, input ->
            val name = input.ingredientName.trim()
            if (name.isEmpty() || input.quantity <= 0.0) return@mapIndexedNotNull null
            val ingredientId = ingredientDao.findByName(name)?.id
                ?: ingredientDao.insert(IngredientEntity(name = name, defaultUnit = input.unit))
            RecipeIngredientEntity(
                recipeId = recipeId,
                ingredientId = ingredientId,
                quantity = input.quantity,
                unit = input.unit,
                position = index
            )
        }
        if (rows.isNotEmpty()) recipeDao.insertRecipeIngredients(rows)
        recipeId
    }

    suspend fun searchIngredientNames(query: String): List<String> =
        if (query.isBlank()) emptyList() else ingredientDao.search(query).map { it.name }
}
