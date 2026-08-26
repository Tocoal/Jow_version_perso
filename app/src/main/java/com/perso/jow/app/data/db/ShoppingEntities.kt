package com.perso.jow.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class ShoppingSessionStatus {
    /** Recipes are being picked and the shopping list is being checked off in-store. */
    SHOPPING,
    /** Shopping is done; the selected recipes are "en cours" and ready to be cooked. */
    COOKING,
    /** Every recipe in the session has been cooked. */
    DONE
}

@Entity(tableName = "shopping_sessions")
data class ShoppingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val status: ShoppingSessionStatus = ShoppingSessionStatus.SHOPPING,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shopping_session_recipes",
    foreignKeys = [
        ForeignKey(entity = ShoppingSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("sessionId"), Index("recipeId")]
)
data class ShoppingSessionRecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val recipeId: Long,
    val servingsMultiplier: Double = 1.0,
    val isCooked: Boolean = false
)

data class ShoppingSessionRecipeDetail(
    @Embedded val sessionRecipe: ShoppingSessionRecipeEntity,
    @Relation(parentColumn = "recipeId", entityColumn = "id") val recipe: RecipeEntity
)

@Entity(
    tableName = "shopping_list_items",
    foreignKeys = [
        ForeignKey(entity = ShoppingSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = IngredientEntity::class, parentColumns = ["id"], childColumns = ["ingredientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("sessionId"), Index("ingredientId")]
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val ingredientId: Long,
    val quantity: Double,
    val unit: String,
    val isChecked: Boolean = false
)

data class ShoppingListItemDetail(
    @Embedded val item: ShoppingListItemEntity,
    @Relation(parentColumn = "ingredientId", entityColumn = "id") val ingredient: IngredientEntity
)
