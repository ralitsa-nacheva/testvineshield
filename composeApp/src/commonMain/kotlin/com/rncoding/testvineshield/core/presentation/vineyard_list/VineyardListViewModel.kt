package com.rncoding.testvineshield.core.presentation.vineyard_list

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VineyardListViewModel: ViewModel() {
    private val _state = MutableStateFlow(VineyardListState())
    val state = _state.asStateFlow()

    fun onAction(action: VineyardListAction) {
        when(action) {
            is VineyardListAction.onVineyardClick -> {

            }
            is VineyardListAction.onSearchQueryChange -> {
                _state.update {
                    it.copy(searchQuery = action.query)
                }

            }

        }
    }
}