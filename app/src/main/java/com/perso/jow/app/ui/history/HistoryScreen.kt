package com.perso.jow.app.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.perso.jow.app.data.db.HistoryEntryEntity
import com.perso.jow.app.data.repository.HistoryRepository
import com.perso.jow.app.di.AppContainer
import com.perso.jow.app.ui.localAppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel(historyRepository: HistoryRepository) : ViewModel() {
    val entries: StateFlow<List<HistoryEntryEntity>> = historyRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { HistoryViewModel(container.historyRepository) }
        }
    }
}

private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val container = localAppContainer()
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
    val entries by viewModel.entries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Text(
                "Aucune recette cuisinée pour le moment.",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(entries, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(entry.recipeName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${entry.servings} portions · ${dateFormat.format(Date(entry.cookedAt))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
