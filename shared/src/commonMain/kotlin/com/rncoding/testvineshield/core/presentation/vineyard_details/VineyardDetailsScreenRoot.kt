package com.rncoding.testvineshield.core.presentation.vineyard_details

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun VineyardDetailsScreenRoot(
    vineyardId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit
) {
    val viewModel =
        koinViewModel<VineyardDetailsViewModel>(
            key =
                "vineyard-details-$vineyardId",
            parameters = {
                parametersOf(vineyardId)
            }
        )

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
                is VineyardDetailsEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(
                        event.message
                    )

                VineyardDetailsEvent.VineyardDeleted ->
                    onDeleted()
            }
        }
    }

    VineyardDetailsScreen(
        state = state,
        snackbarHostState =
            snackbarHostState,
        onBack = onBack,
        onEdit = onEdit,
        onDeleteRequest =
            viewModel::requestDelete,
        onDeleteConfirm =
            viewModel::confirmDelete,
        onDeleteCancel =
            viewModel::cancelDelete
    )
}