package com.perso.jow.app.ui.navigation

object Destinations {
    const val HUB = "hub"
    const val LIBRARY = "library?favoritesOnly={favoritesOnly}"
    const val RECIPE_EDITOR = "recipe_editor?recipeId={recipeId}"
    const val RECIPE_DETAIL = "recipe_detail/{recipeId}"
    const val SHOPPING_SELECTION = "shopping_selection"
    const val SHOPPING_LIST = "shopping_list/{sessionId}"
    const val COOKING = "cooking"
    const val COOKING_RECIPE = "cooking_recipe/{sessionId}/{sessionRecipeId}"
    const val HISTORY = "history"

    fun library(favoritesOnly: Boolean = false) = "library?favoritesOnly=$favoritesOnly"
    fun recipeEditor(recipeId: Long = -1L) = "recipe_editor?recipeId=$recipeId"
    fun recipeDetail(recipeId: Long) = "recipe_detail/$recipeId"
    fun shoppingList(sessionId: Long) = "shopping_list/$sessionId"
    fun cookingRecipe(sessionId: Long, sessionRecipeId: Long) = "cooking_recipe/$sessionId/$sessionRecipeId"
}
