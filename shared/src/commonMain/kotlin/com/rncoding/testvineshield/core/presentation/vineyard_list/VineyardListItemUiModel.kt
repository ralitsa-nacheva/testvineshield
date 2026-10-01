package com.rncoding.testvineshield.core.presentation.vineyard_list

data class VineyardListItemUiModel(
    val vineyardId: Long,
    val name: String,
    val location: String,
    val blockCount: Int,
    val latestTemperature: Double?,
    val activeAlert: String?,
    val currentDiseaseCount: Int,
    val lastActivityPriority: String?,
    val lastActivityStatus: String?
)