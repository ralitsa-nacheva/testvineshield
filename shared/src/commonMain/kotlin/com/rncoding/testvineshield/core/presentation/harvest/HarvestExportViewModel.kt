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

data class HarvestExportState(val busy: Boolean = false, val message: String? = null, val error: String? = null)

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
                    is Result.Error -> _state.value = HarvestExportState(error = result.error.userMessage)
                    is Result.Success -> when (val saved = saver.save(result.data)) {
                        DocumentSaveResult.Saved -> _state.value = HarvestExportState(message = "Saved ${result.data.recordCount} harvest records.")
                        DocumentSaveResult.Cancelled -> _state.value = HarvestExportState(message = "Export cancelled.")
                        is DocumentSaveResult.Failed -> _state.value = HarvestExportState(error = saved.message)
                    }
                }
            } catch (e: CancellationException) {
                _state.value = HarvestExportState(message = "Export interrupted. Check the destination before retrying.")
                throw e
            } catch (_: Exception) {
                _state.value = HarvestExportState(error = UnexpectedError().userMessage)
            } finally {
                _state.value = _state.value.copy(busy = false)
            }
        }
    }

    fun cancel() { job?.cancel() }
}
