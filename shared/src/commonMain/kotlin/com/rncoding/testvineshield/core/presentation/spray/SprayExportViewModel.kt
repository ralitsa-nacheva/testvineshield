package com.rncoding.testvineshield.core.presentation.spray

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.export.DocumentExportFormat
import com.rncoding.testvineshield.core.domain.usecases.activity.*
import com.rncoding.testvineshield.core.presentation.export.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface SprayExportNotice {
    data class Saved(val productCount: Int) : SprayExportNotice
    data object Cancelled : SprayExportNotice
    data object Interrupted : SprayExportNotice
}

data class SprayExportState(val snapshot: SpraySnapshot? = null, val loading: Boolean = false, val busy: Boolean = false, val notice: SprayExportNotice? = null, val noSavedApplication: Boolean = false, val error: SprayIssue? = null)

class SprayExportViewModel(private val key: SprayKey, private val get: GetSprayRecordUseCase, private val prepare: PrepareSprayExportUseCase) : ViewModel() {
    private val _state = MutableStateFlow(SprayExportState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    init { load() }
    fun load() {
        if (_state.value.loading || _state.value.busy) return
        _state.update { it.copy(loading = true, snapshot = null, error = null, noSavedApplication = false) }
        viewModelScope.launch {
            try {
                when (val result = get(key)) {
                    is Result.Error -> _state.update { it.copy(error = SprayIssue.Failure(result.error)) }
                    is Result.Success -> if (result.data.record == null) _state.update { it.copy(noSavedApplication = true) }
                        else _state.update { it.copy(snapshot = result.data) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = SprayIssue.Failure(UnexpectedError())) } }
            finally { _state.update { it.copy(loading = false) } }
        }
    }
    fun export(format: DocumentExportFormat, saver: DocumentSaver) {
        if (_state.value.loading || _state.value.busy || _state.value.snapshot == null) return
        _state.update { it.copy(busy = true, notice = null, error = null) }
        job = viewModelScope.launch {
            try {
                when (val result = prepare(key, format)) {
                    is Result.Error -> _state.update { it.copy(error = SprayIssue.Failure(result.error)) }
                    is Result.Success -> when (saver.save(result.data)) {
                        DocumentSaveResult.Saved -> _state.update { it.copy(notice = SprayExportNotice.Saved(result.data.recordCount)) }
                        DocumentSaveResult.Cancelled -> _state.update { it.copy(notice = SprayExportNotice.Cancelled) }
                        is DocumentSaveResult.Failed -> _state.update { it.copy(error = SprayIssue.DocumentSaveFailed) }
                    }
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(notice = SprayExportNotice.Interrupted) }; throw e
            } catch (_: Exception) { _state.update { it.copy(error = SprayIssue.Failure(UnexpectedError())) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    fun cancelExport() { job?.cancel() }
}
