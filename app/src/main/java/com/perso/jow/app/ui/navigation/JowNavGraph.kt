package com.perso.jow.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.perso.jow.app.ui.cooking.CookingRecipeScreen
import com.perso.jow.app.ui.cooking.CookingScreen
import com.perso.jow.app.ui.history.HistoryScreen
import com.perso.jow.app.ui.hub.HubScreen
import com.perso.jow.app.ui.library.LibraryScreen
import com.perso.jow.app.ui.recipe.RecipeDetailScreen
import com.perso.jow.app.ui.recipe.RecipeEditorScreen
import com.perso.jow.app.ui.shopping.ShoppingListScreen
import com.perso.jow.app.ui.shopping.ShoppingSelectionScreen

@Composable
fun JowNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Destinations.HUB) {
        composable(Destinations.HUB) {
            HubScreen(
                onGoToShopping = { navController.navigate(Destinations.SHOPPING_SELECTION) },
                onGoToCooking = { navController.navigate(Destinations.COOKING) },
                onGoToLibrary = { navController.navigate(Destinations.library(false)) },
                onGoToFavorites = { navController.navigate(Destinations.library(true)) },
                onGoToHistory = { navController.navigate(Destinations.HISTORY) }
            )
        }
        composable(
            Destinations.LIBRARY,
            arguments = listOf(navArgument("favoritesOnly") { type = NavType.BoolType; defaultValue = false })
        ) { backStackEntry ->
            val favoritesOnly = backStackEntry.arguments?.getBoolean("favoritesOnly") ?: false
            LibraryScreen(
                favoritesOnly = favoritesOnly,
                onBack = { navController.popBackStack() },
                onAddRecipe = { navController.navigate(Destinations.recipeEditor()) },
                onOpenRecipe = { recipeId -> navController.navigate(Destinations.recipeDetail(recipeId)) }
            )
        }
        composable(
            Destinations.RECIPE_EDITOR,
            arguments = listOf(navArgument("recipeId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: -1L
            RecipeEditorScreen(
                recipeId = recipeId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Destinations.RECIPE_DETAIL,
            arguments = listOf(navArgument("recipeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: return@composable
            RecipeDetailScreen(
                recipeId = recipeId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Destinations.recipeEditor(recipeId)) },
                onDeleted = { navController.popBackStack() }
            )
        }
        composable(Destinations.SHOPPING_SELECTION) {
            ShoppingSelectionScreen(
                onBack = { navController.popBackStack() },
                onGenerated = { sessionId -> navController.navigate(Destinations.shoppingList(sessionId)) }
            )
        }
        composable(
            Destinations.SHOPPING_LIST,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            ShoppingListScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
                onFinishShopping = {
                    navController.navigate(Destinations.COOKING) {
                        popUpTo(Destinations.HUB)
                    }
                }
            )
        }
        composable(Destinations.COOKING) {
            CookingScreen(
                onBack = { navController.popBackStack() },
                onOpenRecipe = { sessionId, sessionRecipeId ->
                    navController.navigate(Destinations.cookingRecipe(sessionId, sessionRecipeId))
                }
            )
        }
        composable(
            Destinations.COOKING_RECIPE,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
                navArgument("sessionRecipeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            val sessionRecipeId = backStackEntry.arguments?.getLong("sessionRecipeId") ?: return@composable
            CookingRecipeScreen(
                sessionId = sessionId,
                sessionRecipeId = sessionRecipeId,
                onBack = { navController.popBackStack() },
                onMarkedCooked = { navController.popBackStack() }
            )
        }
        composable(Destinations.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
