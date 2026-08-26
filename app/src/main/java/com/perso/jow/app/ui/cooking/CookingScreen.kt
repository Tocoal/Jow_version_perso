package com.perso.jow.app.ui.cooking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.perso.jow.app.data.db.ShoppingSessionRecipeDetail
import com.perso.jow.app.data.repository.ShoppingRepository
import com.perso.jow.app.di.AppContainer
import com.perso.jow.app.ui.localAppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CookingViewModel(shoppingRepository: ShoppingRepository) : ViewModel() {

    val sessionId: StateFlow<Long?> = shoppingRepository.observeCookingSession()
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recipes: StateFlow<List<ShoppingSessionRecipeDetail>> = sessionId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else shoppingRepository.observeSessionRecipes(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { CookingViewModel(container.shoppingRepository) }
        }
    }
}

@Composable
fun CookingScreen(onBack: () -> Unit, onOpenRecipe: (Long, Long) -> Unit) {
    val container = localAppContainer()
    val viewModel: CookingViewModel = viewModel(factory = CookingViewModel.factory(container))
    val sessionId by viewModel.sessionId.collectAsState()
    val recipes by viewModel.recipes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recettes en cours") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") }
                }
            )
        }
    ) { padding ->
        if (recipes.isEmpty()) {
            Text(
                "Aucune recette en cours. Fais d'abord tes courses depuis le hub.",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recipes, key = { it.sessionRecipe.id }) { detail ->
                val currentSessionId = sessionId ?: return@items
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable(enabled = !detail.sessionRecipe.isCooked) {
                            onOpenRecipe(currentSessionId, detail.sessionRecipe.id)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(detail.recipe.name)
                            Text("${formatServings(detail.recipe.servings, detail.sessionRecipe.servingsMultiplier)} portions")
                        }
                        if (detail.sessionRecipe.isCooked) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Cuisinée")
                        }
                    }
                }
            }
        }
    }
}

private fun formatServings(baseServings: Int, multiplier: Double): String {
    val value = baseServings * multiplier
    return if (value == value.toLong().toDouble()) value.toLong().toString() else "%.1f".format(value)
}
