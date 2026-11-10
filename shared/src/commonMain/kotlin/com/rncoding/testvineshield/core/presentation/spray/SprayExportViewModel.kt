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

data class SprayExportState(val snapshot: SpraySnapshot? = null, val loading: Boolean = false, val busy: Boolean = false, val message: String? = null, val error: String? = null)

class SprayExportViewModel(private val key: SprayKey, private val get: GetSprayRecordUseCase, private val prepare: PrepareSprayExportUseCase) : ViewModel() {
    private val _state = MutableStateFlow(SprayExportState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    init { load() }
    fun load() {
        if (_state.value.loading || _state.value.busy) return
        _state.update { it.copy(loading = true, snapshot = null, error = null) }
        viewModelScope.launch {
            try {
                when (val result = get(key)) {
                    is Result.Error -> _state.update { it.copy(error = result.error.userMessage) }
                    is Result.Success -> if (result.data.record == null) _state.update { it.copy(error = "There is no saved spray application to export.") }
                        else _state.update { it.copy(snapshot = result.data) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = UnexpectedError().userMessage) } }
            finally { _state.update { it.copy(loading = false) } }
        }
    }
    fun export(format: DocumentExportFormat, saver: DocumentSaver) {
        if (_state.value.loading || _state.value.busy || _state.value.snapshot == null) return
        _state.update { it.copy(busy = true, message = null, error = null) }
        job = viewModelScope.launch {
            try {
                when (val result = prepare(key, format)) {
                    is Result.Error -> _state.update { it.copy(error = result.error.userMessage) }
                    is Result.Success -> when (val saved = saver.save(result.data)) {
                        DocumentSaveResult.Saved -> _state.update { it.copy(message = "Saved one spray application with ${result.data.recordCount} product lines.") }
                        DocumentSaveResult.Cancelled -> _state.update { it.copy(message = "Export cancelled.") }
                        is DocumentSaveResult.Failed -> _state.update { it.copy(error = saved.message) }
                    }
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(message = "Export interrupted. Check the destination before retrying.") }; throw e
            } catch (_: Exception) { _state.update { it.copy(error = UnexpectedError().userMessage) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    fun cancelExport() { job?.cancel() }
}
