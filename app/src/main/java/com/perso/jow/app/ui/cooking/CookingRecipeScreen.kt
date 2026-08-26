package com.perso.jow.app.ui.cooking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.perso.jow.app.data.db.RecipeIngredientDetail
import com.perso.jow.app.data.db.ShoppingSessionRecipeDetail
import com.perso.jow.app.data.repository.RecipeRepository
import com.perso.jow.app.data.repository.ShoppingRepository
import com.perso.jow.app.di.AppContainer
import com.perso.jow.app.domain.formatQuantity
import com.perso.jow.app.ui.localAppContainer
import com.perso.jow.core.unit.MeasureUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CookingRecipeState(
    val detail: ShoppingSessionRecipeDetail? = null,
    val ingredients: List<RecipeIngredientDetail> = emptyList()
)

class CookingRecipeViewModel(
    private val shoppingRepository: ShoppingRepository,
    recipeRepository: RecipeRepository,
    private val sessionId: Long,
    sessionRecipeId: Long
) : ViewModel() {

    private val _isDone = MutableStateFlow(false)
    val isDone: StateFlow<Boolean> = _isDone.asStateFlow()

    val state: StateFlow<CookingRecipeState> = shoppingRepository.observeSessionRecipes(sessionId)
        .map { list -> list.find { it.sessionRecipe.id == sessionRecipeId } }
        .flatMapLatest { detail ->
            if (detail == null) flowOf(CookingRecipeState())
            else recipeRepository.observeIngredients(detail.recipe.id).map { ingredients ->
                CookingRecipeState(detail = detail, ingredients = ingredients)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CookingRecipeState())

    fun markCooked() {
        val detail = state.value.detail ?: return
        viewModelScope.launch {
            shoppingRepository.markRecipeCooked(
                sessionId = sessionId,
                sessionRecipe = detail.sessionRecipe,
                recipeName = detail.recipe.name,
                servings = (detail.recipe.servings * detail.sessionRecipe.servingsMultiplier).toInt().coerceAtLeast(1)
            )
            _isDone.value = true
        }
    }

    companion object {
        fun factory(container: AppContainer, sessionId: Long, sessionRecipeId: Long) = viewModelFactory {
            initializer {
                CookingRecipeViewModel(container.shoppingRepository, container.recipeRepository, sessionId, sessionRecipeId)
            }
        }
    }
}

@Composable
fun CookingRecipeScreen(sessionId: Long, sessionRecipeId: Long, onBack: () -> Unit, onMarkedCooked: () -> Unit) {
    val container = localAppContainer()
    val viewModel: CookingRecipeViewModel = viewModel(
        factory = CookingRecipeViewModel.factory(container, sessionId, sessionRecipeId)
    )
    val state by viewModel.state.collectAsState()
    val isDone by viewModel.isDone.collectAsState()

    LaunchedEffect(isDone) {
        if (isDone) onMarkedCooked()
    }

    val detail = state.detail ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail.recipe.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::markCooked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Marquer comme cuisinée")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Pour ${formatServings(detail.recipe.servings, detail.sessionRecipe.servingsMultiplier)} portions",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item { Text("Ingrédients", style = MaterialTheme.typography.titleMedium) }
            items(state.ingredients, key = { it.line.id }) { ingredient ->
                val scaledQuantity = ingredient.line.quantity * detail.sessionRecipe.servingsMultiplier
                Text("• ${formatQuantity(scaledQuantity, MeasureUnit.fromLabel(ingredient.line.unit))} ${ingredient.ingredient.name}")
            }
            item { Text("Étapes", style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(detail.recipe.steps) { index, step ->
                Text("${index + 1}. $step")
            }
        }
    }
}

private fun formatServings(baseServings: Int, multiplier: Double): String {
    val value = baseServings * multiplier
    return if (value == value.toLong().toDouble()) value.toLong().toString() else "%.1f".format(value)
}
