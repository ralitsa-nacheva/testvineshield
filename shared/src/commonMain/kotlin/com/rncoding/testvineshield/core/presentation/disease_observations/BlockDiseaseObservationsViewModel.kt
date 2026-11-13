package com.rncoding.testvineshield.core.presentation.disease_observations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockDiseaseObservation
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import com.rncoding.testvineshield.core.domain.usecases.disease.ChangeDiseaseOccurrenceStatusUseCase
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.usecases.disease.EditDiseaseOccurrenceDetailsUseCase
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository
import com.rncoding.testvineshield.core.domain.usecases.disease.CreateDiseaseOccurrenceUseCase
import com.rncoding.testvineshield.core.domain.usecases.disease.RecordReferenceSymptomUseCase
import com.rncoding.testvineshield.core.domain.datamodels.CompletedBlockActivity
import com.rncoding.testvineshield.core.domain.datamodels.LinkedObservationActivity
import com.rncoding.testvineshield.core.domain.usecases.disease.ObservationActivityLinkUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class DiseaseObservationsKey(val ownerId: Long, val vineyardId: Long, val blockId: Long)

data class ObservationDialog(
    val observation: BlockDiseaseObservation,
    val kind: Kind,
    val activityToUnlink: LinkedObservationActivity? = null
) {
    enum class Kind { DETAILS, STATUS, REFERENCE_SYMPTOM, REMOVE_REFERENCE_SYMPTOM, LINK_ACTIVITY, UNLINK_ACTIVITY }
}

data class DiseaseObservationsState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val observations: List<BlockDiseaseObservation> = emptyList(),
    val diseases: List<DiseaseDomainModel> = emptyList(),
    val completedActivities: List<CompletedBlockActivity> = emptyList(),
    val activityLinks: Map<Long, List<LinkedObservationActivity>> = emptyMap(),
    val saving: Boolean = false,
    val saveError: String? = null,
    val lastSavedId: Long? = null,
    val dialog: ObservationDialog? = null,
    val dialogError: String? = null
)

private data class ObservationNotebookData(
    val observations: List<BlockDiseaseObservation>,
    val diseases: List<DiseaseDomainModel>,
    val completedActivities: List<CompletedBlockActivity>,
    val activityLinks: Map<Long, List<LinkedObservationActivity>>
)

@OptIn(ExperimentalCoroutinesApi::class)
class BlockDiseaseObservationsViewModel(
    private val key: DiseaseObservationsKey,
    private val users: AuthenticatedUserProvider,
    private val repository: DiseaseRepository,
    private val create: CreateDiseaseOccurrenceUseCase,
    private val changeStatus: ChangeDiseaseOccurrenceStatusUseCase,
    private val editDetails: EditDiseaseOccurrenceDetailsUseCase,
    private val recordReferenceSymptom: RecordReferenceSymptomUseCase,
    private val activityLink: ObservationActivityLinkUseCase
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
                if (user?.userId != key.ownerId) flowOf<ObservationNotebookData?>(null)
                else combine(
                    repository.observeOwnedBlockOccurrences(key.ownerId, key.vineyardId, key.blockId),
                    repository.observePredefinedDiseases(),
                    repository.observeCompletedActivitiesForLink(key.ownerId, key.vineyardId, key.blockId),
                    repository.observeOwnedOccurrenceActivityLinks(key.ownerId, key.vineyardId, key.blockId)
                ) { observations, diseases, activities, links ->
                    ObservationNotebookData(observations, diseases, activities, links.groupBy { it.occurrenceId })
                }
            }.buffer(0).catch { error ->
                if (error is CancellationException) throw error
                saveJob?.cancel()
                _state.value = DiseaseObservationsState(loading = false, loadError = "Disease observations could not be loaded.")
            }.collect { data ->
                if (data == null) {
                    saveJob?.cancel()
                    _state.value = DiseaseObservationsState(loading = false, loadError = "This session is no longer available.")
                } else {
                    _state.update {
                        it.copy(
                            loading = false, loadError = null, observations = data.observations,
                            diseases = data.diseases, completedActivities = data.completedActivities,
                            activityLinks = data.activityLinks
                        )
                    }
                }
            }
        }
    }

    fun openDialog(observationId: Long, kind: ObservationDialog.Kind) {
        val current = _state.value
        if (current.saving || current.loadError != null || current.dialog != null) return
        val observation = current.observations.firstOrNull { it.id == observationId } ?: return
        _state.update { it.copy(dialog = ObservationDialog(observation, kind), dialogError = null) }
    }

    fun closeDialog() {
        if (_state.value.saving) return
        _state.update { it.copy(dialog = null, dialogError = null) }
    }

    fun saveStatus(status: DiseaseOccurrenceStatus) {
        saveDialog(ObservationDialog.Kind.STATUS) { observation ->
            changeStatus(key.ownerId, key.vineyardId, key.blockId, observation, status)
        }
    }

    fun saveDetails(date: LocalDate, severity: Int, notes: String) {
        saveDialog(ObservationDialog.Kind.DETAILS) { observation ->
            editDetails(key.ownerId, key.vineyardId, key.blockId, observation, date, severity, notes)
        }
    }

    fun saveReferenceSymptom(date: LocalDate, notes: String) {
        saveDialog(ObservationDialog.Kind.REFERENCE_SYMPTOM) { observation ->
            recordReferenceSymptom(key.ownerId, key.vineyardId, key.blockId, observation, date, notes)
        }
    }

    fun removeReferenceSymptom() {
        saveDialog(ObservationDialog.Kind.REMOVE_REFERENCE_SYMPTOM) { observation ->
            recordReferenceSymptom.remove(key.ownerId, key.vineyardId, key.blockId, observation)
        }
    }

    fun saveActivityLink(activity: CompletedBlockActivity) {
        saveDialog(ObservationDialog.Kind.LINK_ACTIVITY) { observation ->
            activityLink.link(key.ownerId, key.vineyardId, key.blockId, observation, activity)
        }
    }

    fun openUnlinkDialog(link: LinkedObservationActivity) {
        val current = _state.value
        if (current.saving || current.loadError != null || current.dialog != null) return
        val observation = current.observations.firstOrNull { it.id == link.occurrenceId } ?: return
        _state.update {
            it.copy(
                dialog = ObservationDialog(observation, ObservationDialog.Kind.UNLINK_ACTIVITY, link),
                dialogError = null
            )
        }
    }

    fun removeActivityLink() {
        val link = _state.value.dialog?.activityToUnlink ?: return
        saveDialog(ObservationDialog.Kind.UNLINK_ACTIVITY) { observation ->
            activityLink.unlink(key.ownerId, key.vineyardId, key.blockId, observation.id, link.activityId)
        }
    }

    private fun saveDialog(
        kind: ObservationDialog.Kind,
        write: suspend (BlockDiseaseObservation) -> Result<Unit, AppError>
    ) {
        val current = _state.value
        val dialog = current.dialog ?: return
        if (current.saving || current.loadError != null || dialog.kind != kind) return
        _state.update { it.copy(saving = true, dialogError = null) }
        saveJob = viewModelScope.launch {
            try {
                val result = write(dialog.observation)
                currentCoroutineContext().ensureActive()
                when (result) {
                    is Result.Success -> _state.update { it.copy(dialog = null) }
                    is Result.Error -> _state.update { it.copy(dialogError = result.error.userMessage) }
                }
            } finally {
                _state.update { it.copy(saving = false) }
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
