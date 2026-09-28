package com.rncoding.testvineshield.core.domain.datamodels

import kotlinx.datetime.LocalDate

data class DiseaseAlertDomainModel(
    val alertId: Long,
    val diseaseId: Long,
    val vineyardId: Long,
    val blockId: Long?,
    val createdAt: LocalDate,
    val alert: String,
    val alertStatus: String,
    val alertSeverity: String,
    val phenologicalStage: String
)
