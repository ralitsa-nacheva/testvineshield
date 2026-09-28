package com.rncoding.testvineshield.core.domain.datamodels

data class VineyardWithSummaryAggregate(
    val vineyardId: Long,
    val userId: Long,
    val name: String,
    val country: String,
    val city: String,
    val blockCount: Int, // dao function getBlockCountByVineyard
    val lastActivity: String, //dao function getLastActivityByVineyard
    val latestTemperature: Double, // dao function getLatestHourlyTempByVineyard
    val activeAlert: String, //dao function getLastOpenAlertByVineyard
    val currentDiseases: String //dao function getLastOpenDiseaseOccurrenceForVineyard should it be list?
)
