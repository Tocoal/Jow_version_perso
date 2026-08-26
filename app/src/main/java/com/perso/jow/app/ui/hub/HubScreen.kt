package com.perso.jow.app.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.perso.jow.app.data.repository.ShoppingRepository
import com.perso.jow.app.di.AppContainer
import com.perso.jow.app.ui.localAppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class HubViewModel(shoppingRepository: ShoppingRepository) : ViewModel() {

    private val _shoppingCount = MutableStateFlow(0)
    val shoppingCount: StateFlow<Int> = _shoppingCount.asStateFlow()

    private val _cookingCount = MutableStateFlow(0)
    val cookingCount: StateFlow<Int> = _cookingCount.asStateFlow()

    init {
        shoppingRepository.observeShoppingSession()
            .flatMapLatest { session ->
                if (session == null) flowOf(0)
                else shoppingRepository.observeSessionRecipes(session.id).map { it.size }
            }
            .onEach { _shoppingCount.value = it }
            .launchIn(viewModelScope)

        shoppingRepository.observeCookingSession()
            .flatMapLatest { session ->
                if (session == null) flowOf(0)
                else shoppingRepository.observeSessionRecipes(session.id).map { list -> list.count { !it.sessionRecipe.isCooked } }
            }
            .onEach { _cookingCount.value = it }
            .launchIn(viewModelScope)
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { HubViewModel(container.shoppingRepository) }
        }
    }
}

@Composable
fun HubScreen(
    onGoToShopping: () -> Unit,
    onGoToCooking: () -> Unit,
    onGoToLibrary: () -> Unit,
    onGoToFavorites: () -> Unit,
    onGoToHistory: () -> Unit
) {
    val container = localAppContainer()
    val viewModel: HubViewModel = viewModel(factory = HubViewModel.factory(container))
    val shoppingCount by viewModel.shoppingCount.collectAsState()
    val cookingCount by viewModel.cookingCount.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Mon Jow") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onGoToShopping, modifier = Modifier.fillMaxWidth()) {
                Text(if (shoppingCount > 0) "🛒 Faire les courses ($shoppingCount)" else "🛒 Faire les courses")
            }
            Button(onClick = onGoToCooking, modifier = Modifier.fillMaxWidth()) {
                Text(if (cookingCount > 0) "🍳 Recettes en cours ($cookingCount)" else "🍳 Recettes en cours")
            }
            OutlinedButton(onClick = onGoToLibrary, modifier = Modifier.fillMaxWidth()) {
                Text("📖 Bibliothèque de recettes")
            }
            OutlinedButton(onClick = onGoToFavorites, modifier = Modifier.fillMaxWidth()) {
                Text("⭐ Favoris")
            }
            OutlinedButton(onClick = onGoToHistory, modifier = Modifier.fillMaxWidth()) {
                Text("🕘 Historique")
            }
            Text(
                "Sélectionne des recettes pour préparer tes courses, puis cuisine-les depuis \"Recettes en cours\".",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
