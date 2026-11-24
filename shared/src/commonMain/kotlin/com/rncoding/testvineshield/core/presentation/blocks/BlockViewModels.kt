package com.rncoding.testvineshield.core.presentation.blocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.BlockStageObservation
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.datamodels.units.*
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.usecases.block.*
import com.rncoding.testvineshield.core.domain.usecases.vineyard.GetVineyardUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class BlockListState(val loading: Boolean = true, val blocks: List<BlockDomainModel> = emptyList(), val error: AppError? = null)
class BlockListViewModel(private val vineyardId: Long, private val observe: ObserveOwnedBlocksUseCase): ViewModel() {
    private val _state = MutableStateFlow(BlockListState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    init { reload() }
    fun reload() {
        job?.cancel()
        _state.value = BlockListState()
        job = viewModelScope.launch {
            try { observe(vineyardId).collect { result -> _state.value = when (result) {
                        is Result.Error -> BlockListState(loading = false, error = result.error)
                        is Result.Success -> BlockListState(loading = false, blocks = result.data)
                    } } } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.value = BlockListState(loading = false, error = UnexpectedError()) }
        }
    }
}
data class BlockKey(val vineyardId: Long, val blockId: Long)
data class BlockDetailsState(val loading: Boolean = false, val block: BlockDomainModel? = null, val deleting: Boolean = false, val confirmDelete: Boolean = false, val deleted: Boolean = false, val error: AppError? = null,
    val stageObservations: List<BlockStageObservation> = emptyList(),
    val stageHistoryError: AppError? = null
)
class BlockDetailsViewModel(private val key: BlockKey, private val get: GetBlockDetailsUseCase, private val delete: DeleteBlockUseCase,
    private val getStageHistory: GetRecentBlockStageObservationsUseCase
): ViewModel() {
    private val _state = MutableStateFlow(BlockDetailsState())
    val state = _state.asStateFlow()
    init { load() }
    fun load() {
        if (_state.value.loading || _state.value.deleting || _state.value.deleted) return
        _state.update { it.copy(loading = true, error = null, block = null, confirmDelete = false, stageObservations = emptyList(), stageHistoryError = null) }
        viewModelScope.launch {
            try { when (val result = get(key.vineyardId, key.blockId)) {
                    is Result.Error -> _state.update { it.copy(error = result.error) }
                    is Result.Success -> when (val history = getStageHistory(key.vineyardId, key.blockId)) {
                        is Result.Success -> _state.update {
                            it.copy(block = result.data, stageObservations = history.data)
                        }
                        is Result.Error -> _state.update {
                            it.copy(block = result.data, stageHistoryError = history.error)
                        }
                    }
                } } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = UnexpectedError()) } }
            finally { _state.update { it.copy(loading = false) } }
        }
    }
    fun requestDelete() { if (!_state.value.loading && !_state.value.deleting && _state.value.block!=null) _state.update { it.copy(confirmDelete = true, error = null) } }
    fun cancelDelete() { if (!_state.value.deleting) _state.update { it.copy(confirmDelete = false) } }
    fun confirmDelete() {
        if (!_state.value.confirmDelete || _state.value.deleting || _state.value.block==null) return
        _state.update { it.copy(deleting = true, error = null) }
        viewModelScope.launch {
            try { when (val result = delete(key.vineyardId, key.blockId)) {
                    is Result.Error -> _state.update { it.copy(error = result.error, confirmDelete = false) }
                    is Result.Success -> _state.update { it.copy(deleted = true, confirmDelete = false, block = null, stageObservations = emptyList()) }
                } } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = UnexpectedError(), confirmDelete = false) } }
            finally { _state.update { it.copy(deleting = false) } }
        }
    }
}
data class BlockEditorRequest(val vineyardId: Long, val blockId: Long = 0, val restoredDraft: BlockDraft? = null)
data class BlockEditorState(val draft: BlockDraft = BlockDraft(), val loading: Boolean = false, val saving: Boolean = false, val ready: Boolean = false, val savedId: Long? = null, val errors: Map<BlockField, BlockDraftIssue> = emptyMap(), val error: BlockEditorIssue? = null) {
    val canEdit get() = ready && !loading && !saving && savedId==null
}
class BlockEditorViewModel(
private val request: BlockEditorRequest, private val create: CreateBlockUseCase, private val update: UpdateBlockUseCase, private val get: GetBlockDetailsUseCase, private val getVineyard: GetVineyardUseCase
): ViewModel() {
    private val _state = MutableStateFlow(BlockEditorState())
    val state = _state.asStateFlow()
    init { load() }
    fun load() {
        if (_state.value.loading || _state.value.saving || _state.value.ready) return
        if (request.vineyardId<=0 || request.blockId<0) { _state.update { it.copy(error = BlockEditorIssue.InvalidRequest) }
            return }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                if (request.blockId==0L) when (val result = getVineyard(request.vineyardId)) {
                    is Result.Error -> _state.update { it.copy(error = BlockEditorIssue.Failure(result.error)) }
                    is Result.Success -> _state.update { it.copy(ready = true, draft = request.restoredDraft?: BlockDraft()) }
                } else when (val result = get(request.vineyardId, request.blockId)) {
                    is Result.Error -> _state.update { it.copy(error = BlockEditorIssue.Failure(result.error)) }
                    is Result.Success -> _state.update { it.copy(ready = true, draft = request.restoredDraft?: BlockDraft.from(result.data)) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = BlockEditorIssue.Failure(UnexpectedError())) } }
            finally { _state.update { it.copy(loading = false) } }
        }
    }
    private fun change(field: BlockField? = null, transform: (BlockDraft) -> BlockDraft) {
        if (_state.value.canEdit) _state.update {
            it.copy(
                draft = transform(it.draft),
                errors = if (field == null) it.errors else it.errors - field,
                error = null
            )
        }
    }
    fun text(field: BlockField, value: String) = change(field) { it.changed(field, value) }
    fun areaUnit(value: AreaUnit?) = change { it.copy(areaUnit = value) }
    fun spacingUnit(value: LengthUnit?) = change { it.copy(spacingUnit = value) }
    fun stage(value: PhenologicalStage?) = change { draft ->
        if (draft.stage == value) draft else draft.copy(stage = value, stageObservedOn = "")
    }
    fun save() {
        if (!_state.value.canEdit) return
        val parsed = _state.value.draft.parsed(request.vineyardId, request.blockId)
        val model = parsed.block
        if (model==null) { _state.update { it.copy(errors = parsed.errors, error = BlockEditorIssue.ReviewFields) }
            return }
        _state.update { it.copy(saving = true, error = null, errors = emptyMap()) }
        viewModelScope.launch {
            try {
                val result: Result<Long, AppError> = if (request.blockId==0L) create(model) else when (val updated = update(model)) {
                    is Result.Error -> Result.Error(updated.error)
                    is Result.Success -> Result.Success(request.blockId)
                }
                when (result) {
                    is Result.Error -> _state.update { it.copy(error = BlockEditorIssue.Failure(result.error)) }
                    is Result.Success -> _state.update { it.copy(savedId = result.data) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = BlockEditorIssue.Failure(UnexpectedError())) } }
            finally { _state.update { it.copy(saving = false) } }
        }
    }
}
