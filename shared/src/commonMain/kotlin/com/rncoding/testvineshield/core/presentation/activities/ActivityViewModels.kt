package com.rncoding.testvineshield.core.presentation.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.*
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.usecases.activity.*
import com.rncoding.testvineshield.core.domain.usecases.block.GetBlockDetailsUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.GetVineyardUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ActivityListState(
    val loading: Boolean = true,
    val snapshot: ActivityNotebookSnapshot = ActivityNotebookSnapshot(),
    val error: String? = null
)
class ActivityListViewModel(private val scope: ActivityScope, private val observe: ObserveActivityNotebookUseCase): ViewModel() {
    private val _state = MutableStateFlow(ActivityListState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    init { reload() }
    fun reload() {
        job?.cancel()
        _state.value = ActivityListState()
        job = viewModelScope.launch {
            try {
                observe(scope).collect { result ->
                    _state.value = when(result) {
                        is Result.Error -> ActivityListState(loading=false,error=result.error.userMessage)
                        is Result.Success -> ActivityListState(loading=false,snapshot=result.data)
                    }
                }
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) { _state.value=ActivityListState(loading=false,error=UnexpectedError().userMessage) }
        }
    }
}
data class ActivityKey(val vineyardId: Long, val activityId: Long)
data class ActivityDetailsState(
    val loading: Boolean = false,
    val record: ActivityDomainModel? = null,
    val scopeLabel: String = "",
    val deleting: Boolean = false,
    val confirmDelete: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)
class ActivityDetailsViewModel(
    private val key: ActivityKey,
    private val get: GetActivityDetailsUseCase,
    private val delete: DeleteActivityUseCase,
    private val getBlock: GetBlockDetailsUseCase
): ViewModel() {
    private val _state=MutableStateFlow(ActivityDetailsState())
    val state=_state.asStateFlow()
    init { load() }
    fun load() {
        if(_state.value.loading || _state.value.deleting || _state.value.deleted) return
        _state.update { it.copy(loading=true,record=null,error=null,confirmDelete=false) }
        viewModelScope.launch {
            try {
                when(val result=get(key.vineyardId,key.activityId)) {
                    is Result.Error -> _state.update { it.copy(error=result.error.userMessage) }
                    is Result.Success -> {
                        val record=result.data
                        val blockId=record.blockId
                        val label=if(blockId==null) "Whole vineyard" else when(val block=getBlock(key.vineyardId,blockId)) {
                            is Result.Error -> "Block unavailable"
                            is Result.Success -> block.data.name
                        }
                        _state.update { it.copy(record=record,scopeLabel=label) }
                    }
                }
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) { _state.update { it.copy(error=UnexpectedError().userMessage) } }
            finally { _state.update { it.copy(loading=false) } }
        }
    }
    fun requestDelete() {
        if(!_state.value.loading && !_state.value.deleting && _state.value.record!=null)
            _state.update { it.copy(confirmDelete=true,error=null) }
    }
    fun cancelDelete() { if(!_state.value.deleting) _state.update { it.copy(confirmDelete=false) } }
    fun confirmDelete() {
        if(!_state.value.confirmDelete || _state.value.deleting || _state.value.record==null) return
        _state.update { it.copy(deleting=true,error=null) }
        viewModelScope.launch {
            try {
                when(val result=delete(key.vineyardId,key.activityId)) {
                    is Result.Error -> _state.update { it.copy(error=result.error.userMessage,confirmDelete=false) }
                    is Result.Success -> _state.update { it.copy(deleted=true,record=null,confirmDelete=false) }
                }
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) { _state.update { it.copy(error=UnexpectedError().userMessage,confirmDelete=false) } }
            finally { _state.update { it.copy(deleting=false) } }
        }
    }
}
data class ActivityEditorRequest(
    val scope: ActivityScope,
    val activityId: Long = 0,
    val complete: Boolean = false,
    val restoredDraft: ActivityDraft? = null
)
data class ActivityEditorState(
    val draft: ActivityDraft = ActivityDraft(),
    val scopeLabel: String = "",
    val loading: Boolean = false,
    val saving: Boolean = false,
    val ready: Boolean = false,
    val saved: Boolean = false,
    val errors: Map<ActivityField,String> = emptyMap(),
    val error: String? = null
) { val canEdit get() = ready && !loading && !saving && !saved }
class ActivityEditorViewModel(
    private val request: ActivityEditorRequest,
    private val create: CreateActivityUseCase,
    private val update: UpdateActivityUseCase,
    private val get: GetActivityDetailsUseCase,
    private val getVineyard: GetVineyardUseCase,
    private val getBlock: GetBlockDetailsUseCase
): ViewModel() {
    private val _state=MutableStateFlow(ActivityEditorState())
    val state=_state.asStateFlow()
    private var original: ActivityDomainModel? = null
    init { load() }
    fun load() {
        if(_state.value.loading || _state.value.ready || _state.value.saving) return
        if(request.scope.vineyardId<=0 || request.scope.blockId<0 || request.activityId<0) {
            _state.update { it.copy(error="Invalid activity scope.") }
            return
        }
        _state.update { it.copy(loading=true,error=null) }
        viewModelScope.launch {
            try {
                if(request.activityId>0) when(val result=get(request.scope.vineyardId,request.activityId)) {
                    is Result.Error -> _state.update { it.copy(error=result.error.userMessage) }
                    is Result.Success -> {
                        original=result.data
                        val loaded=ActivityDraft.from(result.data)
                        val blockId=result.data.blockId
                        val scopeLabel=if(blockId==null) "Whole vineyard" else when(val block=getBlock(request.scope.vineyardId,blockId)) {
                            is Result.Error -> { _state.update { it.copy(error=block.error.userMessage) };return@launch }
                            is Result.Success -> block.data.name
                        }
                        _state.update { it.copy(ready=true,scopeLabel=scopeLabel,draft=request.restoredDraft ?: if(request.complete) loaded.copy(status=ActivityStatus.COMPLETED) else loaded) }
                    }
                } else when(val parent=getVineyard(request.scope.vineyardId)) {
                    is Result.Error -> _state.update { it.copy(error=parent.error.userMessage) }
                    is Result.Success -> {
                        if(request.scope.blockId>0) when(val block=getBlock(request.scope.vineyardId,request.scope.blockId)) {
                            is Result.Error -> _state.update { it.copy(error=block.error.userMessage) }
                            is Result.Success -> readyNew(block.data.name)
                        } else readyNew("Whole vineyard")
                    }
                }
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) { _state.update { it.copy(error=UnexpectedError().userMessage) } }
            finally { _state.update { it.copy(loading=false) } }
        }
    }
    private fun readyNew(label: String) { _state.update { it.copy(ready=true,scopeLabel=label,draft=request.restoredDraft ?: ActivityDraft()) } }
    private fun change(field: ActivityField? = null, transform: (ActivityDraft)->ActivityDraft) {
        if(_state.value.canEdit) _state.update { it.copy(draft=transform(it.draft),errors=if(field==null) it.errors else it.errors-field,error=null) }
    }
    fun text(field: ActivityField,value: String)=change(field) { it.changed(field,value) }
    fun status(value: ActivityStatus)=change { it.copy(status=value) }
    fun priority(value: ActivityPriority)=change { it.copy(priority=value) }
    fun stage(value: PhenologicalStage?)=change { it.copy(stage=value) }
    fun save() {
        if(!_state.value.canEdit) return
        val parsed=_state.value.draft.parsed(request.scope.vineyardId,request.scope.blockId.takeIf {it>0},original)
        val record=parsed.record
        if(record==null) { _state.update { it.copy(errors=parsed.errors,error="Review the highlighted fields.") };return }
        _state.update { it.copy(saving=true,error=null,errors=emptyMap()) }
        viewModelScope.launch {
            try {
                val result: Result<Unit,AppError> = if(request.activityId==0L) create(record) else update(record)
                when(result) {
                    is Result.Error -> _state.update { it.copy(error=result.error.userMessage) }
                    is Result.Success -> _state.update { it.copy(saved=true) }
                }
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) { _state.update { it.copy(error=UnexpectedError().userMessage) } }
            finally { _state.update { it.copy(saving=false) } }
        }
    }
}
