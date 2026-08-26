package com.perso.jow.app.ui.shopping

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.perso.jow.app.data.db.ShoppingListItemDetail
import com.perso.jow.app.data.db.ShoppingListItemEntity
import com.perso.jow.app.data.repository.ShoppingRepository
import com.perso.jow.app.di.AppContainer
import com.perso.jow.app.domain.formatQuantity
import com.perso.jow.app.ui.localAppContainer
import com.perso.jow.core.unit.MeasureUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingListViewModel(
    private val shoppingRepository: ShoppingRepository,
    private val sessionId: Long
) : ViewModel() {

    val items: StateFlow<List<ShoppingListItemDetail>> = shoppingRepository.observeListItems(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleChecked(item: ShoppingListItemEntity) {
        viewModelScope.launch { shoppingRepository.setItemChecked(item, !item.isChecked) }
    }

    fun finishShopping(onDone: () -> Unit) {
        viewModelScope.launch {
            shoppingRepository.finishShopping(sessionId)
            onDone()
        }
    }

    companion object {
        fun factory(container: AppContainer, sessionId: Long) = viewModelFactory {
            initializer { ShoppingListViewModel(container.shoppingRepository, sessionId) }
        }
    }
}

@Composable
fun ShoppingListScreen(sessionId: Long, onBack: () -> Unit, onFinishShopping: () -> Unit) {
    val container = localAppContainer()
    val viewModel: ShoppingListViewModel = viewModel(factory = ShoppingListViewModel.factory(container, sessionId))
    val items by viewModel.items.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Liste de courses") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.finishShopping(onFinishShopping) },
                enabled = items.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Courses terminées, direction les fourneaux !")
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Text(
                "Aucun ingrédient dans cette liste.",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(items, key = { it.item.id }) { detail ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = detail.item.isChecked,
                        onCheckedChange = { viewModel.toggleChecked(detail.item) }
                    )
                    Text(
                        "${formatQuantity(detail.item.quantity, MeasureUnit.fromLabel(detail.item.unit))} ${detail.ingredient.name}",
                        textDecoration = if (detail.item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }
        }
    }
}
