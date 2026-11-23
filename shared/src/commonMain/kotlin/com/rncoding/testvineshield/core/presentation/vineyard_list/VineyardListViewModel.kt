package com.rncoding.testvineshield.core.presentation.vineyard_list

import com.rncoding.testvineshield.core.presentation.vineyards.VineyardLoadIssue

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
                    if (throwable is kotlinx.coroutines.CancellationException) throw throwable
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            loadIssue =
                                VineyardLoadIssue.Failed
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
                            loadIssue = null
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
                            result.error
                        )
                    )
                }

                else -> Unit
            }
        }
    }

    fun reorder(vineyardId: Long, offset: Int) {
        val current = _state.value
        if (current.isReordering || current.vineyardPendingDeletion != null ||
            offset == 0
        ) return

        val previousIds = current.vineyards.map { it.vineyardId }
        val fromIndex = previousIds.indexOf(vineyardId)
        val toIndex = fromIndex + offset
        if (fromIndex < 0 || toIndex !in previousIds.indices) return

        val newOrder = previousIds.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        _state.update { it.copy(isReordering = true) }
        viewModelScope.launch {
            try {
                when (val result = reorderVineyardsUseCase(vineyardIds = newOrder)) {
                    is Result.Error -> _events.send(VineyardListEvent.ShowSnackbar(result.error))
                    is Result.Success -> _state.update { latest ->
                        if (latest.vineyards.map { it.vineyardId } == previousIds) {
                            val byId = latest.vineyards.associateBy { it.vineyardId }
                            latest.copy(vineyards = newOrder.map(byId::getValue))
                        } else latest
                    }
                }
            } finally {
                _state.update { it.copy(isReordering = false) }
            }
        }
    }
}
