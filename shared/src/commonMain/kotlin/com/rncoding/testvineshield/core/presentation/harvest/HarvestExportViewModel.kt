package com.rncoding.testvineshield.core.presentation.harvest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.export.HarvestExportFormat
import com.rncoding.testvineshield.core.domain.usecases.harvest.HarvestScope
import com.rncoding.testvineshield.core.domain.usecases.harvest.PrepareHarvestExportUseCase
import com.rncoding.testvineshield.core.presentation.export.DocumentSaver
import com.rncoding.testvineshield.core.presentation.export.DocumentSaveResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface HarvestExportNotice {
    data class Saved(val recordCount: Int) : HarvestExportNotice
    data object Cancelled : HarvestExportNotice
    data object Interrupted : HarvestExportNotice
}

data class HarvestExportState(val busy: Boolean = false, val notice: HarvestExportNotice? = null, val error: HarvestIssue? = null)

class HarvestExportViewModel(private val prepare: PrepareHarvestExportUseCase) : ViewModel() {
    private val _state = MutableStateFlow(HarvestExportState())
    val state = _state.asStateFlow()
    private var job: Job? = null

    fun export(scope: HarvestScope, season: String?, format: HarvestExportFormat, saver: DocumentSaver) {
        if (_state.value.busy) return
        _state.value = HarvestExportState(busy = true)
        job = viewModelScope.launch {
            try {
                when (val result = prepare(scope, season, format)) {
                    is Result.Error -> _state.value = HarvestExportState(error = HarvestIssue.Failure(result.error))
                    is Result.Success -> when (saver.save(result.data)) {
                        DocumentSaveResult.Saved -> _state.value = HarvestExportState(notice = HarvestExportNotice.Saved(result.data.recordCount))
                        DocumentSaveResult.Cancelled -> _state.value = HarvestExportState(notice = HarvestExportNotice.Cancelled)
                        is DocumentSaveResult.Failed -> _state.value = HarvestExportState(error = HarvestIssue.DocumentSaveFailed)
                    }
                }
            } catch (e: CancellationException) {
                _state.value = HarvestExportState(notice = HarvestExportNotice.Interrupted)
                throw e
            } catch (_: Exception) {
                _state.value = HarvestExportState(error = HarvestIssue.Failure(UnexpectedError()))
            } finally {
                _state.value = _state.value.copy(busy = false)
            }
        }
    }

    fun cancel() { job?.cancel() }
}
