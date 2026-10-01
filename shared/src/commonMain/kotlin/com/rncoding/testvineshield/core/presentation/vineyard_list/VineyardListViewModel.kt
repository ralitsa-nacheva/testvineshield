package com.rncoding.testvineshield.core.presentation.vineyard_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.vineyard.DeleteVineyardUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.ObserveVineyardSummariesUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.ReorderVineyardsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch

class VineyardListViewModel(
    private val observeVineyardSummariesUseCase:
    ObserveVineyardSummariesUseCase,
    private val deleteVineyardUseCase:
    DeleteVineyardUseCase,
    private val reorderVineyardsUseCase:
    ReorderVineyardsUseCase,
    private val mapper:
    VineyardListMapper
) : ViewModel() {

    private val _state =
        MutableStateFlow(
            VineyardListUiState()
        )

    val state: StateFlow<VineyardListUiState> =
        _state.asStateFlow()

    private val _events =
        Channel<VineyardListEvent>(
            capacity = Channel.BUFFERED
        )

    val events =
        _events.receiveAsFlow()

    init {
        observeVineyards()
    }

    private fun observeVineyards() {

        viewModelScope.launch {
            observeVineyardSummariesUseCase()
                .catch { throwable ->
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage =
                                throwable.message
                                    ?: "Unable to load vineyards."
                        )
                    }
                }
                .collect { summaries ->
                    _state.update { current ->
                        current.copy(
                            vineyards =
                                summaries.map(
                                    mapper::toUiModel
                                ),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun requestDelete(
        vineyardId: Long
    ) {

        _state.update {
            it.copy(
                vineyardPendingDeletion =
                    vineyardId
            )
        }
    }

    fun cancelDelete() {

        _state.update {
            it.copy(
                vineyardPendingDeletion = null
            )
        }
    }

    fun confirmDelete() {

        val vineyardId =
            _state.value.vineyardPendingDeletion
                ?: return

        _state.update {
            it.copy(
                vineyardPendingDeletion = null
            )
        }

        viewModelScope.launch {

            when (
                val result =
                    deleteVineyardUseCase(
                        vineyardId
                    )
            ) {

                is Result.Success<*> -> {
                    _events.send(
                        VineyardListEvent.VineyardDeleted
                    )
                }

                is Result.Error<*> -> {
                    _events.send(
                        VineyardListEvent.ShowSnackbar(
                            result.error.userMessage
                        )
                    )
                }

                else -> Unit
            }
        }
    }

    fun reorder(
        fromIndex: Int,
        toIndex: Int
    ) {

        val previousOrder =
            _state.value.vineyards

        if (fromIndex !in previousOrder.indices ||
            toIndex !in previousOrder.indices ||
            fromIndex == toIndex
        ) {
            return
        }

        val reordered =
            previousOrder.toMutableList().apply {
                val moved = removeAt(fromIndex)
                add(toIndex, moved)
            }
        /*
         * Optimistic UI update.
         *
         * The list moves immediately rather than waiting
         * for Room to finish writing.
         */
        _state.update {
            it.copy(
                vineyards = reordered,
                isReordering = true
            )
        }

        viewModelScope.launch {

            val result =
                reorderVineyardsUseCase(
                    vineyardIds =
                        reordered.map {
                            it.vineyardId
                        }
                )

            when (result) {

                is Result.Error<*> -> {
                    _state.update {
                        it.copy(
                            vineyards = previousOrder,
                            isReordering = false
                        )
                    }

                    _events.send(
                        VineyardListEvent.ShowSnackbar(
                            result.error.userMessage
                        )
                    )
                }

                else -> Unit
            }
        }
    }
}