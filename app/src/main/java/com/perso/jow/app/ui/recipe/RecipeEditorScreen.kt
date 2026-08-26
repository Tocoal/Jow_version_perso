package com.perso.jow.app.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.perso.jow.app.data.repository.RecipeIngredientInput
import com.perso.jow.app.data.repository.RecipeRepository
import com.perso.jow.app.di.AppContainer
import com.perso.jow.app.domain.UNIT_OPTIONS
import com.perso.jow.app.domain.displayLabel
import com.perso.jow.app.ui.localAppContainer
import com.perso.jow.core.unit.MeasureUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IngredientLineState(
    val key: Int,
    val name: String = "",
    val quantity: String = "",
    val unit: MeasureUnit = MeasureUnit.GRAM
)

data class RecipeEditorState(
    val name: String = "",
    val description: String = "",
    val servings: String = "4",
    val category: String = "",
    val isFavorite: Boolean = false,
    val steps: List<String> = listOf(""),
    val ingredients: List<IngredientLineState> = listOf(IngredientLineState(0)),
    val activeSuggestionKey: Int? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)

class RecipeEditorViewModel(
    private val recipeRepository: RecipeRepository,
    private val recipeId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeEditorState())
    val state: StateFlow<RecipeEditorState> = _state.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private var nextKey = 1

    init {
        if (recipeId <= 0L) {
            _state.update { it.copy(isLoading = false) }
        } else {
            viewModelScope.launch {
                val recipe = recipeRepository.observeRecipe(recipeId).first()
                val ingredients = recipeRepository.getIngredients(recipeId)
                if (recipe != null) {
                    val lines = ingredients.mapIndexed { index, detail ->
                        IngredientLineState(
                            key = index,
                            name = detail.ingredient.name,
                            quantity = formatPlain(detail.line.quantity),
                            unit = MeasureUnit.fromLabel(detail.line.unit)
                        )
                    }.ifEmpty { listOf(IngredientLineState(0)) }
                    nextKey = (lines.maxOfOrNull { it.key } ?: 0) + 1
                    _state.value = RecipeEditorState(
                        name = recipe.name,
                        description = recipe.description,
                        servings = recipe.servings.toString(),
                        category = recipe.category ?: "",
                        isFavorite = recipe.isFavorite,
                        steps = recipe.steps.ifEmpty { listOf("") },
                        ingredients = lines,
                        isLoading = false
                    )
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun updateName(value: String) = _state.update { it.copy(name = value) }
    fun updateDescription(value: String) = _state.update { it.copy(description = value) }
    fun updateServings(value: String) = _state.update { it.copy(servings = value.filter(Char::isDigit)) }
    fun updateCategory(value: String) = _state.update { it.copy(category = value) }
    fun toggleFavorite() = _state.update { it.copy(isFavorite = !it.isFavorite) }

    fun updateStep(index: Int, value: String) = _state.update { s ->
        s.copy(steps = s.steps.toMutableList().also { it[index] = value })
    }

    fun addStep() = _state.update { it.copy(steps = it.steps + "") }

    fun removeStep(index: Int) = _state.update { s ->
        val updated = s.steps.toMutableList().also { it.removeAt(index) }
        s.copy(steps = updated.ifEmpty { listOf("") })
    }

    fun updateIngredientName(key: Int, value: String) {
        _state.update { s ->
            s.copy(
                ingredients = s.ingredients.map { if (it.key == key) it.copy(name = value) else it },
                activeSuggestionKey = key
            )
        }
        viewModelScope.launch { _suggestions.value = recipeRepository.searchIngredientNames(value) }
    }

    fun selectIngredientSuggestion(key: Int, name: String) {
        _state.update { s ->
            s.copy(
                ingredients = s.ingredients.map { if (it.key == key) it.copy(name = name) else it },
                activeSuggestionKey = null
            )
        }
        _suggestions.value = emptyList()
    }

    fun updateIngredientQuantity(key: Int, value: String) = _state.update { s ->
        s.copy(ingredients = s.ingredients.map { if (it.key == key) it.copy(quantity = value) else it })
    }

    fun updateIngredientUnit(key: Int, unit: MeasureUnit) = _state.update { s ->
        s.copy(ingredients = s.ingredients.map { if (it.key == key) it.copy(unit = unit) else it })
    }

    fun addIngredientLine() = _state.update { it.copy(ingredients = it.ingredients + IngredientLineState(nextKey++)) }

    fun removeIngredientLine(key: Int) = _state.update { s ->
        val updated = s.ingredients.filterNot { it.key == key }
        s.copy(ingredients = updated.ifEmpty { listOf(IngredientLineState(nextKey++)) })
    }

    fun save() {
        val current = _state.value
        if (current.name.isBlank()) return
        viewModelScope.launch {
            val recipe = RecipeEntity(
                id = if (recipeId > 0) recipeId else 0,
                name = current.name.trim(),
                description = current.description.trim(),
                servings = current.servings.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                steps = current.steps.map(String::trim).filter { it.isNotEmpty() },
                category = current.category.trim().ifEmpty { null },
                isFavorite = current.isFavorite
            )
            val ingredientInputs = current.ingredients.mapNotNull { line ->
                val quantity = line.quantity.replace(',', '.').toDoubleOrNull() ?: return@mapNotNull null
                RecipeIngredientInput(ingredientName = line.name, quantity = quantity, unit = line.unit.label)
            }
            recipeRepository.saveRecipe(recipe, ingredientInputs)
            _state.update { it.copy(isSaved = true) }
        }
    }

    companion object {
        fun factory(container: AppContainer, recipeId: Long) = viewModelFactory {
            initializer { RecipeEditorViewModel(container.recipeRepository, recipeId) }
        }
    }
}

private fun formatPlain(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(recipeId: Long, onSaved: () -> Unit, onBack: () -> Unit) {
    val container = localAppContainer()
    val viewModel: RecipeEditorViewModel = viewModel(factory = RecipeEditorViewModel.factory(container, recipeId))
    val state by viewModel.state.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recipeId > 0) "Modifier la recette" else "Nouvelle recette") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favori"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) return@Scaffold

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("Nom de la recette") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.servings,
                        onValueChange = viewModel::updateServings,
                        label = { Text("Portions") },
                        modifier = Modifier.width(120.dp)
                    )
                    OutlinedTextField(
                        value = state.category,
                        onValueChange = viewModel::updateCategory,
                        label = { Text("Catégorie") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Text("Ingrédients", style = MaterialTheme.typography.titleMedium) }
            items(state.ingredients, key = { it.key }) { line ->
                IngredientLineEditor(
                    line = line,
                    suggestions = if (state.activeSuggestionKey == line.key) suggestions else emptyList(),
                    onNameChange = { viewModel.updateIngredientName(line.key, it) },
                    onSuggestionSelected = { viewModel.selectIngredientSuggestion(line.key, it) },
                    onQuantityChange = { viewModel.updateIngredientQuantity(line.key, it) },
                    onUnitChange = { viewModel.updateIngredientUnit(line.key, it) },
                    onRemove = { viewModel.removeIngredientLine(line.key) }
                )
            }
            item {
                TextButton(onClick = viewModel::addIngredientLine) { Text("+ Ajouter un ingrédient") }
            }

            item { Text("Étapes", style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(state.steps) { index, step ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = step,
                        onValueChange = { viewModel.updateStep(index, it) },
                        label = { Text("Étape ${index + 1}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    IconButton(onClick = { viewModel.removeStep(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Supprimer l'étape")
                    }
                }
            }
            item {
                TextButton(onClick = viewModel::addStep) { Text("+ Ajouter une étape") }
            }

            item {
                androidx.compose.material3.Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.name.isNotBlank()
                ) { Text("Enregistrer") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientLineEditor(
    line: IngredientLineState,
    suggestions: List<String>,
    onNameChange: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (MeasureUnit) -> Unit,
    onRemove: () -> Unit
) {
    Column {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = line.name,
                onValueChange = onNameChange,
                label = { Text("Ingrédient") },
                modifier = Modifier.weight(1.5f)
            )
            OutlinedTextField(
                value = line.quantity,
                onValueChange = onQuantityChange,
                label = { Text("Qté") },
                modifier = Modifier.weight(0.7f)
            )
            var unitMenuExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = unitMenuExpanded,
                onExpandedChange = { unitMenuExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = line.unit.displayLabel(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unité") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(expanded = unitMenuExpanded, onDismissRequest = { unitMenuExpanded = false }) {
                    UNIT_OPTIONS.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayLabel) },
                            onClick = {
                                onUnitChange(option.unit)
                                unitMenuExpanded = false
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Supprimer l'ingrédient")
            }
        }
        if (suggestions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.take(5).forEach { suggestion ->
                    AssistChip(onClick = { onSuggestionSelected(suggestion) }, label = { Text(suggestion) })
                }
            }
        }
    }
}
