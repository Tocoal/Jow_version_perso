package com.perso.jow.app.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.perso.jow.app.data.db.RecipeEntity
import com.perso.jow.app.data.repository.RecipeRepository
import com.perso.jow.app.data.repository.ShoppingRepository
import com.perso.jow.app.di.AppContainer
import com.perso.jow.app.ui.localAppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShoppingSelectionState(
    val recipes: List<RecipeEntity> = emptyList(),
    val selections: Map<Long, Double> = emptyMap()
)

class ShoppingSelectionViewModel(
    private val recipeRepository: RecipeRepository,
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    private val _selections = MutableStateFlow<Map<Long, Double>>(emptyMap())

    private val _generatedSessionId = MutableStateFlow<Long?>(null)
    val generatedSessionId: StateFlow<Long?> = _generatedSessionId.asStateFlow()

    val state: StateFlow<ShoppingSelectionState> = combine(
        recipeRepository.observeRecipes(),
        _selections
    ) { recipes, selections -> ShoppingSelectionState(recipes = recipes, selections = selections) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShoppingSelectionState())

    init {
        viewModelScope.launch {
            val session = shoppingRepository.observeShoppingSession().first()
            if (session != null) {
                val recipes = shoppingRepository.observeSessionRecipes(session.id).first()
                _selections.value = recipes.associate { it.sessionRecipe.recipeId to it.sessionRecipe.servingsMultiplier }
            }
        }
    }

    fun toggleRecipe(recipeId: Long) {
        _selections.update { current ->
            if (current.containsKey(recipeId)) current - recipeId else current + (recipeId to 1.0)
        }
    }

    fun updateMultiplier(recipeId: Long, delta: Double) {
        _selections.update { current ->
            val existing = current[recipeId] ?: return@update current
            current + (recipeId to (existing + delta).coerceAtLeast(0.5))
        }
    }

    fun generateShoppingList() {
        val selections = _selections.value
        if (selections.isEmpty()) return
        viewModelScope.launch {
            val session = shoppingRepository.getOrCreateShoppingSession()
            shoppingRepository.setSessionRecipes(session.id, selections.map { it.key to it.value })
            _generatedSessionId.value = session.id
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ShoppingSelectionViewModel(container.recipeRepository, container.shoppingRepository) }
        }
    }
}

@Composable
fun ShoppingSelectionScreen(onBack: () -> Unit, onGenerated: (Long) -> Unit) {
    val container = localAppContainer()
    val viewModel: ShoppingSelectionViewModel = viewModel(factory = ShoppingSelectionViewModel.factory(container))
    val state by viewModel.state.collectAsState()
    val generatedSessionId by viewModel.generatedSessionId.collectAsState()

    LaunchedEffect(generatedSessionId) {
        generatedSessionId?.let(onGenerated)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choisir les recettes") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::generateShoppingList,
                enabled = state.selections.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Générer la liste de courses (${state.selections.size})")
            }
        }
    ) { padding ->
        if (state.recipes.isEmpty()) {
            Text(
                "Ajoute des recettes à ta bibliothèque pour pouvoir faire tes courses.",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(state.recipes, key = { it.id }) { recipe ->
                val multiplier = state.selections[recipe.id]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = multiplier != null, onCheckedChange = { viewModel.toggleRecipe(recipe.id) })
                        Column {
                            Text(recipe.name)
                            Text("${recipe.servings} portions de base")
                        }
                    }
                    if (multiplier != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.updateMultiplier(recipe.id, -0.5) }) {
                                Icon(Icons.Filled.Remove, contentDescription = "Diminuer")
                            }
                            Text("×${formatMultiplier(multiplier)}")
                            IconButton(onClick = { viewModel.updateMultiplier(recipe.id, 0.5) }) {
                                Icon(Icons.Filled.Add, contentDescription = "Augmenter")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMultiplier(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
