package com.rncoding.testvineshield.core.presentation.disease_observations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockDiseaseObservation
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository
import com.rncoding.testvineshield.core.domain.usecases.disease.CreateDiseaseOccurrenceUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class DiseaseObservationsKey(val ownerId: Long, val vineyardId: Long, val blockId: Long)

data class DiseaseObservationsState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val observations: List<BlockDiseaseObservation> = emptyList(),
    val diseases: List<DiseaseDomainModel> = emptyList(),
    val saving: Boolean = false,
    val saveError: String? = null,
    val lastSavedId: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class BlockDiseaseObservationsViewModel(
    private val key: DiseaseObservationsKey,
    private val users: AuthenticatedUserProvider,
    private val repository: DiseaseRepository,
    private val create: CreateDiseaseOccurrenceUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(DiseaseObservationsState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null
    private var saveJob: Job? = null

    init { reload() }

    fun reload() {
        if (loadJob?.isActive == true || _state.value.saving) return
        _state.value = DiseaseObservationsState()
        loadJob = viewModelScope.launch {
            users.observeUser().flatMapLatest { user ->
                if (user?.userId != key.ownerId) flowOf<Pair<List<BlockDiseaseObservation>, List<DiseaseDomainModel>>?>(null)
                else combine(
                    repository.observeOwnedBlockOccurrences(key.ownerId, key.vineyardId, key.blockId),
                    repository.observePredefinedDiseases()
                ) { observations, diseases -> observations to diseases }
            }.buffer(0).catch { error ->
                if (error is CancellationException) throw error
                saveJob?.cancel()
                _state.value = DiseaseObservationsState(loading = false, loadError = "Disease observations could not be loaded.")
            }.collect { data ->
                if (data == null) {
                    saveJob?.cancel()
                    _state.value = DiseaseObservationsState(loading = false, loadError = "This session is no longer available.")
                } else {
                    _state.update { it.copy(loading = false, loadError = null, observations = data.first, diseases = data.second) }
                }
            }
        }
    }

    fun save(diseaseId: Long, date: LocalDate, severity: Int, notes: String) {
        val current = _state.value
        if (current.loading || current.saving || current.loadError != null ||
            current.diseases.none { it.diseaseId == diseaseId }
        ) return
        _state.update { it.copy(saving = true, saveError = null) }
        saveJob = viewModelScope.launch {
            try {
                val result = create(key.ownerId, key.vineyardId, key.blockId, diseaseId, date, severity, notes)
                currentCoroutineContext().ensureActive()
                when (result) {
                    is Result.Success -> _state.update { it.copy(lastSavedId = result.data) }
                    is Result.Error -> _state.update { it.copy(saveError = result.error.userMessage) }
                }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }
}
