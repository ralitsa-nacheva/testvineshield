package com.rncoding.testvineshield.core.presentation.spray

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.datamodels.SprayQuantityUnit
import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.usecases.activity.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SprayRequest(val key: SprayKey, val restoredDraft: SprayDraft? = null)
data class SprayState(
    val snapshot: SpraySnapshot? = null, val draft: SprayDraft = SprayDraft(),
    val editing: Boolean = false, val loading: Boolean = false, val saving: Boolean = false, val deleting: Boolean = false,
    val saved: Boolean = false, val removed: Boolean = false, val confirmDelete: Boolean = false,
    val errors: Map<String, String> = emptyMap(), val error: String? = null
) {
    val busy get() = loading || saving || deleting
    val canEdit get() = snapshot != null && editing && !busy && !saved && !removed
}

class SprayViewModel(
    private val request: SprayRequest,
    private val get: GetSprayRecordUseCase,
    private val saveRecord: SaveSprayRecordUseCase,
    private val deleteRecord: DeleteSprayRecordUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(SprayState())
    val state = _state.asStateFlow()
    private var restore = true
    init { load() }
    fun refreshIfViewing() { if (!_state.value.editing) load() }
    fun load(discardDraft: Boolean = false) {
        if (_state.value.busy || _state.value.saved || _state.value.removed || (_state.value.editing && !discardDraft)) return
        _state.update { it.copy(loading = true, snapshot = null, error = null, confirmDelete = false) }
        viewModelScope.launch {
            try {
                when (val result = get(request.key)) {
                    is Result.Error -> _state.update { it.copy(error = result.error.userMessage) }
                    is Result.Success -> {
                        val loaded = result.data
                        val restored = if (restore && !discardDraft) request.restoredDraft else null
                        restore = false
                        val draft = restored ?: loaded.record?.let(SprayDraft::from) ?: SprayDraft(zone = loaded.vineyardTimeZone)
                        val conflict = restored != null && (restored.baseRevision != (loaded.record?.revision ?: 0) || restored.baseGeneration != loaded.record?.generation)
                        _state.update { it.copy(snapshot = loaded, draft = draft, editing = restored != null || loaded.record == null,
                            errors = emptyMap(), error = if (conflict) "Saved data changed since this draft. Copy any needed text, then reload the saved record before saving." else null) }
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = UnexpectedError().userMessage) } }
            finally { _state.update { it.copy(loading = false) } }
        }
    }
    fun edit() { if (!_state.value.busy && _state.value.snapshot != null) _state.update { it.copy(editing = true, error = null) } }
    private fun change(transform: (SprayDraft) -> SprayDraft) {
        if (_state.value.canEdit) _state.update { it.copy(draft = transform(it.draft), errors = emptyMap(), error = null) }
    }
    fun text(field: SprayField, value: String) = change { it.changed(field, value) }
    fun zone(value: String) = change { it.copy(zone = value) }
    fun areaUnit(value: AreaUnit?) = change { it.copy(areaUnit = value) }
    fun waterUnit(value: SprayQuantityUnit?) = change { it.copy(waterUnit = value) }
    fun addLine() = change { if (it.lines.size < 20) it.copy(lines = it.lines + SprayLineDraft()) else it }
    fun removeLine(index: Int) = change { if (it.lines.size > 1 && index in it.lines.indices) it.copy(lines = it.lines.filterIndexed { at, _ -> at != index }) else it }
    fun lineText(index: Int, field: SprayLineField, value: String) = change { draft ->
        draft.copy(lines = draft.lines.mapIndexed { at, line -> if (at != index) line else when (field) {
            SprayLineField.Name -> line.copy(name = value); SprayLineField.Quantity -> line.copy(quantity = value); SprayLineField.Reference -> line.copy(reference = value)
        } })
    }
    fun lineUnit(index: Int, value: SprayQuantityUnit?) = change { draft -> draft.copy(lines = draft.lines.mapIndexed { at, line -> if (at == index) line.copy(unit = value) else line }) }
    fun save() {
        if (!_state.value.canEdit) return
        val parsed = _state.value.draft.parsed(request.key.activityId)
        val record = parsed.record
        if (record == null) { _state.update { it.copy(errors = parsed.errors, error = "Review the highlighted fields.") }; return }
        _state.update { it.copy(saving = true, error = null, errors = emptyMap()) }
        viewModelScope.launch {
            try {
                when (val result = saveRecord(request.key, record)) {
                    is Result.Error -> _state.update { it.copy(error = result.error.userMessage) }
                    is Result.Success -> _state.update { it.copy(saved = true) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = UnexpectedError().userMessage) } }
            finally { _state.update { it.copy(saving = false) } }
        }
    }
    fun requestDelete() { if (!_state.value.busy && !_state.value.editing && _state.value.snapshot?.record != null) _state.update { it.copy(confirmDelete = true, error = null) } }
    fun cancelDelete() { if (!_state.value.deleting) _state.update { it.copy(confirmDelete = false) } }
    fun confirmDelete() {
        val record = _state.value.snapshot?.record ?: return
        val generation = record.generation ?: return
        if (_state.value.busy || !_state.value.confirmDelete) return
        _state.update { it.copy(deleting = true, error = null) }
        viewModelScope.launch {
            try {
                when (val result = deleteRecord(request.key, record.revision, generation)) {
                    is Result.Error -> _state.update { it.copy(error = result.error.userMessage, confirmDelete = false) }
                    is Result.Success -> _state.update { it.copy(removed = true, confirmDelete = false) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = UnexpectedError().userMessage, confirmDelete = false) } }
            finally { _state.update { it.copy(deleting = false) } }
        }
    }
}
