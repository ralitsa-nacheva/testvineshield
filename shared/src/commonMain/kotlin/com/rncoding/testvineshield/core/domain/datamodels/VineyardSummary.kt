package com.rncoding.testvineshield.core.domain.datamodels

data class VineyardSummary(
    val vineyardId: Long,
    val name: String,
    val country: String,
    val city: String,
    val blockCount: Int,
    val lastActivityPriority: String?,
    val lastActivityStatus: String?,
    val latestTemperature: Double?,
    val activeAlert: String?,
    val currentDiseaseCount: Int,
    val sortOrder: Int
)