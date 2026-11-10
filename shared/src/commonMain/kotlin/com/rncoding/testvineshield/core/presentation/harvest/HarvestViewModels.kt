package com.rncoding.testvineshield.core.presentation.harvest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.usecases.block.GetBlockDetailsUseCase
import com.rncoding.testvineshield.core.domain.usecases.harvest.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HarvestListState(
    val loading: Boolean = true,
    val snapshot: HarvestNotebookSnapshot? = null,
    val error: String? = null
)

class HarvestListViewModel(private val scope: HarvestScope, private val observe: ObserveOwnedHarvestsUseCase) : ViewModel() {
    private val _state = MutableStateFlow(HarvestListState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    init { reload() }
    fun reload() {
        job?.cancel()
        _state.value = HarvestListState()
        job = viewModelScope.launch {
            try {
                observe(scope).collect { result ->
                    _state.value = when (result) {
                        is Result.Error -> HarvestListState(loading = false, error = result.error.userMessage)
                        is Result.Success -> HarvestListState(loading = false, snapshot = result.data)
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.value = HarvestListState(loading = false, error = UnexpectedError().userMessage) }
        }
    }
}

data class HarvestKey(val scope: HarvestScope, val harvestId: Long)
data class HarvestDetailsState(
    val loading: Boolean = false,
    val record: HarvestDomainModel? = null,
    val deleting: Boolean = false,
    val confirmDelete: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)

class HarvestDetailsViewModel(
    private val key: HarvestKey,
    private val get: GetHarvestDetailsUseCase,
    private val delete: DeleteHarvestUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(HarvestDetailsState())
    val state = _state.asStateFlow()
    init { load() }
    fun load() {
        if (_state.value.loading || _state.value.deleting || _state.value.deleted) return
        _state.update { it.copy(loading = true, record = null, error = null, confirmDelete = false) }
        viewModelScope.launch {
            try {
                when (val result = get(key.scope, key.harvestId)) {
                    is Result.Error -> _state.update { it.copy(error = result.error.userMessage) }
                    is Result.Success -> _state.update { it.copy(record = result.data) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = UnexpectedError().userMessage) } }
            finally { _state.update { it.copy(loading = false) } }
        }
    }
    fun requestDelete() {
        if (!_state.value.loading && !_state.value.deleting && _state.value.record != null)
            _state.update { it.copy(confirmDelete = true, error = null) }
    }
    fun cancelDelete() {
        if (!_state.value.deleting) _state.update { it.copy(confirmDelete = false) }
    }
    fun confirmDelete() {
        if (!_state.value.confirmDelete || _state.value.deleting || _state.value.record == null) return
        _state.update { it.copy(deleting = true, error = null) }
        viewModelScope.launch {
            try {
                when (val result = delete(key.scope.vineyardId, key.scope.blockId, key.harvestId)) {
                    is Result.Error -> _state.update { it.copy(error = result.error.userMessage, confirmDelete = false) }
                    is Result.Success -> _state.update { it.copy(deleted = true, record = null, confirmDelete = false) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = UnexpectedError().userMessage, confirmDelete = false) } }
            finally { _state.update { it.copy(deleting = false) } }
        }
    }
}

data class HarvestEditorRequest(val key: HarvestKey, val restoredDraft: HarvestDraft? = null)
data class HarvestEditorState(
    val draft: HarvestDraft = HarvestDraft(),
    val blockName: String = "",
    val legacyUnit: String? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val ready: Boolean = false,
    val saved: Boolean = false,
    val errors: Map<HarvestField, String> = emptyMap(),
    val unitError: String? = null,
    val error: String? = null
) {
    val canEdit get() = ready && !loading && !saving && !saved
}

class HarvestEditorViewModel(
    private val request: HarvestEditorRequest,
    private val getBlock: GetBlockDetailsUseCase,
    private val get: GetHarvestDetailsUseCase,
    private val create: CreateHarvestUseCase,
    private val update: UpdateHarvestUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(HarvestEditorState())
    val state = _state.asStateFlow()
    private var original: HarvestDomainModel? = null
    init { load() }
    fun load() {
        if (_state.value.loading || _state.value.ready || _state.value.saving || _state.value.saved) return
        val key = request.key
        if (key.scope.vineyardId <= 0 || key.scope.blockId <= 0 || key.harvestId < 0) {
            _state.update { it.copy(error = "Invalid harvest scope.") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                when (val block = getBlock(key.scope.vineyardId, key.scope.blockId)) {
                    is Result.Error -> _state.update { it.copy(error = block.error.userMessage) }
                    is Result.Success -> {
                        val loaded = if (key.harvestId == 0L) null else when (val result = get(key.scope, key.harvestId)) {
                            is Result.Error -> { _state.update { it.copy(error = result.error.userMessage) }; return@launch }
                            is Result.Success -> result.data
                        }
                        original = loaded
                        _state.update { it.copy(ready = true, blockName = block.data.name, legacyUnit = loaded?.units?.takeIf { value -> value.isNotBlank() },
                            draft = request.restoredDraft ?: loaded?.let(HarvestDraft::from) ?: HarvestDraft()) }
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = UnexpectedError().userMessage) } }
            finally { _state.update { it.copy(loading = false) } }
        }
    }
    fun text(field: HarvestField, value: String) {
        if (_state.value.canEdit) _state.update { it.copy(draft = it.draft.changed(field, value), errors = it.errors - field, error = null) }
    }
    fun units(value: String?) {
        if (_state.value.canEdit) _state.update { it.copy(draft = it.draft.copy(units = value), unitError = null, error = null) }
    }
    fun save() {
        if (!_state.value.canEdit) return
        val parsed = _state.value.draft.parsed(request.key.scope, original)
        val record = parsed.record
        if (record == null) {
            _state.update { it.copy(errors = parsed.errors, unitError = parsed.unitError, error = "Review the highlighted fields.") }
            return
        }
        _state.update { it.copy(saving = true, errors = emptyMap(), unitError = null, error = null) }
        viewModelScope.launch {
            try {
                if (request.key.harvestId == 0L) {
                    when (val result = create(record, request.key.scope.vineyardId)) {
                        is Result.Error -> _state.update { it.copy(error = result.error.userMessage) }
                        is Result.Success -> _state.update { it.copy(saved = true) }
                    }
                } else {
                    when (val result = update(record, request.key.scope.vineyardId)) {
                        is Result.Error -> _state.update { it.copy(error = result.error.userMessage) }
                        is Result.Success -> _state.update { it.copy(saved = true) }
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = UnexpectedError().userMessage) } }
            finally { _state.update { it.copy(saving = false) } }
        }
    }
}
