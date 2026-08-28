package com.rncoding.testvineshield.core.presentation.vineyard_list

import com.rncoding.testvineshield.core.presentation.UIText
import com.rncoding.testvineshield.vineyard_details.domain.Vineyard

data class VineyardListState(
    val searchQuery: String = "VineyardEntity",
    val searchResults: List<Vineyard> = emptyList(),
    val isLoading: Boolean = false,
    val selectedIndex: Int, // for the selected tab, do I need another tab or only one with the vineyards list?
    val errorMessage: UIText? = null

)
