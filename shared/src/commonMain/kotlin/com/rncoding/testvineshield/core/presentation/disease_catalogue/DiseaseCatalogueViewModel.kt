package com.rncoding.testvineshield.core.presentation.disease_catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed interface DiseaseCatalogueState {
    data object Loading : DiseaseCatalogueState
    data object Unavailable : DiseaseCatalogueState
    data class Loaded(val diseases: List<DiseaseDomainModel>) : DiseaseCatalogueState
}

class DiseaseCatalogueViewModel(private val repository: DiseaseRepository) : ViewModel() {
    private val _state = MutableStateFlow<DiseaseCatalogueState>(DiseaseCatalogueState.Loading)
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init { reload() }

    fun reload() {
        if (loadJob?.isActive == true) return
        _state.value = DiseaseCatalogueState.Loading
        loadJob = viewModelScope.launch {
            try {
                repository.observePredefinedDiseases().collect {
                    _state.value = DiseaseCatalogueState.Loaded(it)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.value = DiseaseCatalogueState.Unavailable
            }
        }
    }
}
