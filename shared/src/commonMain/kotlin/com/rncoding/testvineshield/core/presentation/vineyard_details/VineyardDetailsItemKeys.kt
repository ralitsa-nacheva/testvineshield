package com.rncoding.testvineshield.core.presentation.vineyard_details

/** One LazyColumn shares a key namespace across all of its sections. */
internal object VineyardDetailsItemKeys {
    const val INFORMATION = "vineyard-information"
    const val WEATHER = "vineyard-weather"
    const val BLOCKS_HEADING = "vineyard-blocks-heading"
    const val BLOCKS_EMPTY = "vineyard-blocks-empty"
    const val DISEASES_HEADING = "vineyard-diseases-heading"
    const val DISEASES_EMPTY = "vineyard-diseases-empty"
    const val ALERTS_HEADING = "vineyard-alerts-heading"
    const val ALERTS_EMPTY = "vineyard-alerts-empty"
    const val DELETE = "vineyard-delete-action"

    fun block(id: Long): String = "block-$id"
    fun occurrence(id: Long): String = "occurrence-$id"
    fun alert(id: Long): String = "alert-$id"
}
