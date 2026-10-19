package com.rncoding.testvineshield.core.domain.disease_risk

import kotlinx.datetime.LocalDate

data class DiseaseRiskMonitoringConfiguration(
    val vineyardId: Long,
    val blockId: Long,
    val diseaseId: Long,
    val modelType: DiseaseRiskModelType,
    val monitoringStartDate: LocalDate,
    val enabled: Boolean = true
)