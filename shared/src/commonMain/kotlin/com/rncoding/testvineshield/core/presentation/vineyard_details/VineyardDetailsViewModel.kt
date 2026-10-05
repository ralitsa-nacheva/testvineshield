package com.rncoding.testvineshield.core.presentation.vineyard_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.vineyard.DeleteVineyardUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.ObserveVineyardDetailsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VineyardDetailsViewModel(
    private val vineyardId: Long,
    private val observeVineyardDetailsUseCase: ObserveVineyardDetailsUseCase,
    private val deleteVineyardUseCase: DeleteVineyardUseCase,
    private val vineyardDetailsUiMapper: VineyardDetailsUiMapper
) : ViewModel() {

    private val _state =
        MutableStateFlow(
            VineyardDetailsUiState()
        )

    val state: StateFlow<VineyardDetailsUiState> =
        _state.asStateFlow()

    private val _events =
        Channel<VineyardDetailsEvent>(
            capacity = Channel.BUFFERED
        )

    val events =
        _events.receiveAsFlow()

    init {
        observeDetails()
    }

    fun requestDelete() {
        if (_state.value.isDeleting) {
            return
        }

        _state.update {
            it.copy(
                showDeleteConfirmation = true
            )
        }
    }

    fun cancelDelete() {
        if (_state.value.isDeleting) {
            return
        }

        _state.update {
            it.copy(
                showDeleteConfirmation = false
            )
        }
    }

    fun confirmDelete() {
        val current = _state.value

        if (
            current.isDeleting ||
            !current.showDeleteConfirmation
        ) {
            return
        }

        _state.update {
            it.copy(
                isDeleting = true
            )
        }

        viewModelScope.launch {
            when (
                val result =
                    deleteVineyardUseCase(vineyardId)
            ) {
                is Result.Success<*> -> {
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            showDeleteConfirmation = false
                        )
                    }

                    _events.send(
                        VineyardDetailsEvent.VineyardDeleted
                    )
                }

                is Result.Error<*> -> {
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            showDeleteConfirmation = false
                        )
                    }

                    _events.send(
                        VineyardDetailsEvent.ShowSnackbar(
                            result.error.userMessage
                        )
                    )
                }

                else -> {
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            showDeleteConfirmation = false
                        )
                    }

                    _events.send(
                        VineyardDetailsEvent.ShowSnackbar(
                            "Unable to delete the vineyard."
                        )
                    )
                }
            }
        }
    }

    private fun observeDetails() {
        viewModelScope.launch {
            observeVineyardDetailsUseCase(vineyardId)
                .catch { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage =
                                throwable.message
                                    ?: "Unable to load the vineyard."
                        )
                    }
                }
                .collect { summary ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            details =
                                summary?.let(
                                    vineyardDetailsUiMapper::toUi
                                ),
                            errorMessage =
                                if (summary == null) {
                                    "Vineyard could not be found."
                                } else {
                                    null
                                }
                        )
                    }
                }
        }
    }
}