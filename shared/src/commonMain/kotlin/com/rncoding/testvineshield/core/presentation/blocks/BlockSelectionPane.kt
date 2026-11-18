package com.rncoding.testvineshield.core.presentation.blocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import org.koin.core.parameter.parametersOf

@Composable
internal fun BlockSelectionPane(
    ownerId: Long,
    vineyardId: Long,
    selectedBlockId: Long,
    enabled: Boolean,
    onSelect: (Long) -> Unit
) {
    val viewModel = koinViewModel<BlockListViewModel>(
        key = "block-selection-$ownerId-$vineyardId",
        parameters = { parametersOf(vineyardId) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchText by rememberSaveable(ownerId, vineyardId) { mutableStateOf("") }
    val activity = LocalUserActivity.current
    val query = searchText.trim()
    val visibleRecords = remember(state.blocks, query) {
        state.blocks.filter { it.name.contains(query, true) || it.vineVariety.contains(query, true) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("Blocks", style = MaterialTheme.typography.titleLarge) }
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { activity(); searchText = it },
                label = { Text("Search blocks") },
                enabled = enabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        when {
            state.loading -> item { Text("Loading blocks…") }
            state.error != null -> item {
                Text(state.error.orEmpty())
                TextButton(onClick = userAction(viewModel::reload), enabled = enabled) { Text("Retry") }
            }
            state.blocks.isEmpty() -> item { Text("No blocks saved.") }
            visibleRecords.isEmpty() -> item { Text("No matches.") }
            else -> items(visibleRecords, key = { it.blockId }) { block ->
                val isSelected = block.blockId == selectedBlockId
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().semantics { selected = isSelected },
                    onClick = userAction { onSelect(block.blockId) },
                    enabled = enabled
                ) {
                    Text(
                        block.name,
                        style = if (isSelected) MaterialTheme.typography.titleSmall
                        else MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
