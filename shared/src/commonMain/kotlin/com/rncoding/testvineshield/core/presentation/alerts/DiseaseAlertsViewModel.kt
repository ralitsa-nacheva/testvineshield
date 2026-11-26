package com.rncoding.testvineshield.core.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseAlertInboxItem
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.alerts.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DiseaseAlertsState(
    val loading: Boolean = true,
    val rows: List<DiseaseAlertInboxItem> = emptyList(),
    val error: AppError? = null,
    val savingId: Long? = null
)

class DiseaseAlertsViewModel(
    private val ownerId: Long,
    private val observe: ObserveOwnedDiseaseAlertsUseCase,
    private val acknowledge: AcknowledgeDiseaseAlertUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(DiseaseAlertsState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null
    private var writeJob: Job? = null
    init { reload() }
    fun reload() {
        if (_state.value.savingId != null) return
        loadJob?.cancel()
        _state.value = DiseaseAlertsState()
        loadJob = viewModelScope.launch {
            observe(ownerId).collect { result ->
                when (result) {
                    is Result.Error -> {
                        writeJob?.cancel()
                        _state.value = DiseaseAlertsState(loading = false, error = result.error)
                    }
                    is Result.Success -> _state.update { it.copy(loading = false, rows = result.data) }
                }
            }
        }
    }
    fun markReviewed(expected: DiseaseAlertInboxItem) {
        val current = _state.value
        if (current.loading || current.savingId != null || current.rows.none { it == expected }) return
        _state.update { it.copy(savingId = expected.alertId, error = null) }
        writeJob = viewModelScope.launch {
            try {
                val result = acknowledge(ownerId, expected)
                currentCoroutineContext().ensureActive()
                if (result is Result.Error) _state.update { it.copy(error = result.error) }
                // Stored Flow supplies the new status; never fabricate a successful local status.
            } finally {
                _state.update { it.copy(savingId = null) }
            }
        }
    }
}
