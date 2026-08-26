package com.perso.jow.app.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.perso.jow.app.data.db.RecipeEntity
import com.perso.jow.app.data.db.RecipeIngredientDetail
import com.perso.jow.app.data.repository.RecipeRepository
import com.perso.jow.app.di.AppContainer
import com.perso.jow.app.domain.formatQuantity
import com.perso.jow.app.ui.localAppContainer
import com.perso.jow.core.unit.MeasureUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeDetailState(
    val recipe: RecipeEntity? = null,
    val ingredients: List<RecipeIngredientDetail> = emptyList(),
    val isDeleted: Boolean = false
)

class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository,
    private val recipeId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeDetailState())
    val state: StateFlow<RecipeDetailState> = _state.asStateFlow()

    init {
        combine(
            recipeRepository.observeRecipe(recipeId),
            recipeRepository.observeIngredients(recipeId)
        ) { recipe, ingredients -> RecipeDetailState(recipe = recipe, ingredients = ingredients) }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)
    }

    fun toggleFavorite() {
        val recipe = _state.value.recipe ?: return
        viewModelScope.launch { recipeRepository.setFavorite(recipe.id, !recipe.isFavorite) }
    }

    fun delete() {
        val recipe = _state.value.recipe ?: return
        viewModelScope.launch {
            recipeRepository.deleteRecipe(recipe)
            _state.update { it.copy(isDeleted = true) }
        }
    }

    companion object {
        fun factory(container: AppContainer, recipeId: Long) = viewModelFactory {
            initializer { RecipeDetailViewModel(container.recipeRepository, recipeId) }
        }
    }
}

@Composable
fun RecipeDetailScreen(recipeId: Long, onBack: () -> Unit, onEdit: () -> Unit, onDeleted: () -> Unit) {
    val container = localAppContainer()
    val viewModel: RecipeDetailViewModel = viewModel(factory = RecipeDetailViewModel.factory(container, recipeId))
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (state.isDeleted) {
        onDeleted()
        return
    }

    val recipe = state.recipe ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (recipe.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favori"
                        )
                    }
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Modifier") }
                    IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Text("${recipe.servings} portions", style = MaterialTheme.typography.bodyLarge) }
            if (recipe.description.isNotBlank()) {
                item { Text(recipe.description, style = MaterialTheme.typography.bodyMedium) }
            }
            item { Text("Ingrédients", style = MaterialTheme.typography.titleMedium) }
            items(state.ingredients, key = { it.line.id }) { detail ->
                Text("• ${formatQuantity(detail.line.quantity, MeasureUnit.fromLabel(detail.line.unit))} ${detail.ingredient.name}")
            }
            item { Text("Étapes", style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(recipe.steps) { index, step ->
                Text("${index + 1}. $step")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer la recette ?") },
            text = { Text("Cette action est définitive.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }
}
