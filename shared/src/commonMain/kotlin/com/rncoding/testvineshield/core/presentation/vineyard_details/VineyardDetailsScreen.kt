package com.rncoding.testvineshield.core.presentation.vineyard_details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VineyardDetailsScreen(
    state: VineyardDetailsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.vineyard?.name
                            ?: "Vineyard"
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack
                    ) {
                        Text("Back")
                    }
                },
                actions = {
                    if (state.vineyard != null) {
                        TextButton(
                            onClick = onEdit,
                            enabled = !state.isDeleting
                        ) {
                            Text("Edit")
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState =
                    snackbarHostState
            )
        }
    ) { innerPadding ->

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment =
                        Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text = state.errorMessage
                    )
                }
            }

            state.vineyard != null -> {
                val vineyard =
                    state.vineyard

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text =
                            "${vineyard.city}, ${vineyard.country}"
                    )

                    Text(
                        text =
                            "Size: ${vineyard.size}"
                    )

                    Text(
                        text =
                            "Latitude: ${vineyard.latitude}"
                    )

                    Text(
                        text =
                            "Longitude: ${vineyard.longitude}"
                    )

                    Text(
                        text =
                            "Elevation: ${vineyard.elevation} m"
                    )

                    Text(
                        text =
                            "Time zone: ${vineyard.timeZone}"
                    )

                    OutlinedButton(
                        onClick = onDeleteRequest,
                        enabled = !state.isDeleting,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        if (state.isDeleting) {
                            CircularProgressIndicator()
                        } else {
                            Text("Delete vineyard")
                        }
                    }
                }
            }
        }
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isDeleting) {
                    onDeleteCancel()
                }
            },
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
                    onClick = onDeleteConfirm,
                    enabled = !state.isDeleting
                ) {
                    if (state.isDeleting) {
                        CircularProgressIndicator()
                    } else {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDeleteCancel,
                    enabled = !state.isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}