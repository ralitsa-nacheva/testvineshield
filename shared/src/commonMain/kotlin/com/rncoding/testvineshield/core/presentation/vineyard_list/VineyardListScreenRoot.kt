package com.rncoding.testvineshield.core.presentation.vineyard_list

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VineyardListScreenRoot(
    onVineyardClick: (Long) -> Unit,
    onCreateVineyard: () -> Unit
) {

    val viewModel:
            VineyardListViewModel =
        koinViewModel()

    val state by
    viewModel.state
        .collectAsStateWithLifecycle()

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    LaunchedEffect(viewModel) {

        viewModel.events.collect { event ->

            when (event) {

                is VineyardListEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        event.message
                    )
                }

                VineyardListEvent.VineyardDeleted -> {
                    snackbarHostState.showSnackbar(
                        "Vineyard deleted."
                    )
                }
            }
        }
    }

    VineyardListScreen(
        state = state,
        snackbarHostState =
            snackbarHostState,
        onVineyardClick =
            onVineyardClick,
        onCreateVineyard =
            onCreateVineyard,
        onDeleteRequest =
            viewModel::requestDelete,
        onDeleteConfirm =
            viewModel::confirmDelete,
        onDeleteCancel =
            viewModel::cancelDelete,
        onReorder =
            viewModel::reorder
    )
}