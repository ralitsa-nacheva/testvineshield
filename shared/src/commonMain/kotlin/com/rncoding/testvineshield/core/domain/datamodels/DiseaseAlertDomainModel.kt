package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertSeverity
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import kotlinx.datetime.LocalDate

data class DiseaseAlertDomainModel(
    val alertId: Long,
    val diseaseId: Long,
    val vineyardId: Long,
    val blockId: Long?,
    val createdAt: LocalDate,
    val alert: String,
    val alertStatus: AlertStatus,
    val alertSeverity: AlertSeverity,
    val phenologicalStage: PhenologicalStage
)
