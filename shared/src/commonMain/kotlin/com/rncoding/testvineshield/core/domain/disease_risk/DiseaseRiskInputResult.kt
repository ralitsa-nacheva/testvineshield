package com.rncoding.testvineshield.core.domain.disease_risk

sealed interface DiseaseRiskInputResult {

    data class Success(
        val input: DiseaseRiskInput
    ) : DiseaseRiskInputResult

    data class MissingVineyard(
        val vineyardId: Long
    ) : DiseaseRiskInputResult

    data class InvalidTimeZone(
        val timeZone: String?
    ) : DiseaseRiskInputResult

    data class InsufficientWeather(
        val reason: String
    ) : DiseaseRiskInputResult

    data class Failure(
        val reason: String
    ) : DiseaseRiskInputResult

    data class MissingMonitoringConfiguration(
        val vineyardId: Long,
        val blockId: Long,
        val diseaseId: Long,
        val modelType: DiseaseRiskModelType
    ) : DiseaseRiskInputResult

    data class MonitoringDisabled(
        val vineyardId: Long,
        val blockId: Long,
        val diseaseId: Long,
        val modelType: DiseaseRiskModelType
    ) : DiseaseRiskInputResult

    data object NoNewWeather : DiseaseRiskInputResult
}