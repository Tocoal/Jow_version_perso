package com.perso.jow.app.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.perso.jow.app.data.db.RecipeEntity
import com.perso.jow.app.data.repository.RecipeRepository
import com.perso.jow.app.di.AppContainer
import com.perso.jow.app.ui.components.RecipeCard
import com.perso.jow.app.ui.localAppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val recipeRepository: RecipeRepository,
    favoritesOnly: Boolean
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val source = if (favoritesOnly) recipeRepository.observeFavorites() else recipeRepository.observeRecipes()

    val recipes: StateFlow<List<RecipeEntity>> = combine(source, _query) { recipes, query ->
        if (query.isBlank()) recipes else recipes.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun toggleFavorite(recipe: RecipeEntity) {
        viewModelScope.launch { recipeRepository.setFavorite(recipe.id, !recipe.isFavorite) }
    }

    companion object {
        fun factory(container: AppContainer, favoritesOnly: Boolean) = viewModelFactory {
            initializer { LibraryViewModel(container.recipeRepository, favoritesOnly) }
        }
    }
}

@Composable
fun LibraryScreen(
    favoritesOnly: Boolean,
    onBack: () -> Unit,
    onAddRecipe: () -> Unit,
    onOpenRecipe: (Long) -> Unit
) {
    val container = localAppContainer()
    val viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(container, favoritesOnly))
    val recipes by viewModel.recipes.collectAsState()
    val query by viewModel.query.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (favoritesOnly) "Favoris" else "Bibliothèque de recettes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!favoritesOnly) {
                FloatingActionButton(onClick = onAddRecipe) {
                    Icon(Icons.Filled.Add, contentDescription = "Ajouter une recette")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Rechercher une recette") },
                modifier = Modifier
                    .fillMaxWidthPadded()
            )
            if (recipes.isEmpty()) {
                Text(
                    if (favoritesOnly) "Aucune recette favorite pour le moment." else "Aucune recette. Appuie sur + pour en ajouter une.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidthPadded()
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(recipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = { onOpenRecipe(recipe.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(recipe) },
                            modifier = Modifier.fillMaxWidthPadded()
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.fillMaxWidthPadded() = this.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
