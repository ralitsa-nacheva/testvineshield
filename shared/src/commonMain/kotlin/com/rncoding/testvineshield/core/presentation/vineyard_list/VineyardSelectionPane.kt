package com.rncoding.testvineshield.core.presentation.vineyard_list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rncoding.testvineshield.core.presentation.root.LocalUserActivity
import com.rncoding.testvineshield.core.presentation.root.userAction
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun VineyardSelectionPane(
    userId: Long,
    selectedVineyardId: Long,
    enabled: Boolean,
    onSelect: (Long) -> Unit
) {
    val viewModel = koinViewModel<VineyardListViewModel>(key = "vineyard-selection-pane-$userId")
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchText by rememberSaveable(userId) { mutableStateOf("") }
    val activity = LocalUserActivity.current
    val query = searchText.trim()
    val visibleRecords = remember(state.vineyards, query) {
        state.vineyards.filter { it.name.contains(query, true) || it.location.contains(query, true) }
    }


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("Vineyards", style = MaterialTheme.typography.titleLarge) }
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { activity(); searchText = it },
                label = { Text("Search vineyards") },
                enabled = enabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        when {
            state.isLoading -> item { Text("Loading vineyards…") }
            state.vineyards.isEmpty() -> item { Text(state.errorMessage ?: "No vineyards saved.") }
            visibleRecords.isEmpty() -> item { Text("No matches.") }
            else -> items(visibleRecords, key = { it.vineyardId }) { vineyard ->
                val isSelected = vineyard.vineyardId == selectedVineyardId
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().semantics { selected = isSelected },
                    onClick = userAction { onSelect(vineyard.vineyardId) },
                    enabled = enabled
                ) {
                    Text(
                        vineyard.name,
                        style = if (isSelected) MaterialTheme.typography.titleSmall
                        else MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
