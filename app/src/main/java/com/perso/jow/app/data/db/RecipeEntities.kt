package com.perso.jow.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val servings: Int = 4,
    val steps: List<String> = emptyList(),
    val category: String? = null,
    val imageUri: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ingredients", indices = [Index(value = ["name"], unique = true)])
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultUnit: String = "g"
)

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = IngredientEntity::class, parentColumns = ["id"], childColumns = ["ingredientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("recipeId"), Index("ingredientId")]
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val ingredientId: Long,
    val quantity: Double,
    val unit: String,
    val position: Int = 0
)

/** A recipe ingredient line together with the ingredient it points to, for display. */
data class RecipeIngredientDetail(
    @Embedded val line: RecipeIngredientEntity,
    @Relation(parentColumn = "ingredientId", entityColumn = "id") val ingredient: IngredientEntity
)
