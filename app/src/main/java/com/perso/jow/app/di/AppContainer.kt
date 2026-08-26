package com.perso.jow.app.di

import android.content.Context
import com.perso.jow.app.data.db.AppDatabase
import com.perso.jow.app.data.repository.HistoryRepository
import com.perso.jow.app.data.repository.RecipeRepository
import com.perso.jow.app.data.repository.ShoppingRepository

/** Minimal hand-rolled DI container: one instance per process, held by [com.perso.jow.app.JowApplication]. */
class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)

    val recipeRepository = RecipeRepository(database)
    val shoppingRepository = ShoppingRepository(database)
    val historyRepository = HistoryRepository(database)
}
