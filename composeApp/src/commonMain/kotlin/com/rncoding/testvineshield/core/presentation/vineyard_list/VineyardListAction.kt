package com.rncoding.testvineshield.core.presentation.vineyard_list

import com.rncoding.testvineshield.vineyard_details.domain.Vineyard

sealed interface VineyardListAction {
    data class onSearchQueryChange(val query: String): VineyardListAction
    data class onVineyardClick(val vineyard: Vineyard): VineyardListAction

}