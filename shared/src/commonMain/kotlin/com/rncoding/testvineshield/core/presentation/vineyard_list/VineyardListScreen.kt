package com.rncoding.testvineshield.core.presentation.vineyard_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VineyardListScreen(
    state: VineyardListUiState,
    snackbarHostState: SnackbarHostState,
    onVineyardClick: (Long) -> Unit,
    onCreateVineyard: () -> Unit,
    onDeleteRequest: (Long) -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteCancel: () -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Vineyards")
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateVineyard
            ) {
                Text("+")
            }
        }
    ) { innerPadding ->

        when {
            state.isLoading -> {
                VineyardListLoading(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            state.errorMessage != null &&
                    state.vineyards.isEmpty() -> {
                VineyardListError(
                    message = state.errorMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            state.vineyards.isEmpty() -> {
                EmptyVineyardList(
                    onCreateVineyard = onCreateVineyard,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.vineyards,
                        key = { vineyard ->
                            vineyard.vineyardId
                        }
                    ) { vineyard ->
                        VineyardListItem(
                            vineyard = vineyard,
                            onClick = {
                                onVineyardClick(
                                    vineyard.vineyardId
                                )
                            },
                            onDelete = {
                                onDeleteRequest(
                                    vineyard.vineyardId
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (state.vineyardPendingDeletion != null) {
        AlertDialog(
            onDismissRequest = onDeleteCancel,
            title = {
                Text("Delete vineyard?")
            },
            text = {
                Text(
                    "This will permanently delete the vineyard " +
                            "and its related local data."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onDeleteConfirm
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDeleteCancel
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun VineyardListItem(
    vineyard: VineyardListItemUiModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text = vineyard.name
                )

                TextButton(
                    onClick = onDelete
                ) {
                    Text("Delete")
                }
            }

            if (vineyard.location.isNotBlank()) {
                Text(
                    text = vineyard.location
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = when (vineyard.blockCount) {
                    1 -> "1 block"
                    else -> "${vineyard.blockCount} blocks"
                }
            )

            vineyard.latestTemperature?.let { temperature ->
                Text(
                    text = "Temperature: $temperature °C"
                )
            }

            vineyard.lastActivityStatus?.let { status ->
                val priority =
                    vineyard.lastActivityPriority
                        ?.takeIf { it.isNotBlank() }

                Text(
                    text = if (priority != null) {
                        "Last activity: $status · $priority"
                    } else {
                        "Last activity: $status"
                    }
                )
            }

            vineyard.activeAlert?.let { alert ->
                if (alert.isNotBlank()) {
                    Text(
                        text = "Alert: $alert"
                    )
                }
            }

            if (vineyard.currentDiseaseCount > 0) {
                Text(
                    text =
                        "Active diseases: " +
                                vineyard.currentDiseaseCount
                )
            }
        }
    }
}

@Composable
private fun VineyardListLoading(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyVineyardList(
    onCreateVineyard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = "No vineyards yet."
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onCreateVineyard
            ) {
                Text("Create vineyard")
            }
        }
    }
}

@Composable
private fun VineyardListError(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message
        )
    }
}