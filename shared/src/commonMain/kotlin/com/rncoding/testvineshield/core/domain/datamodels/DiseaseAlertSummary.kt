package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertSeverity
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import kotlinx.datetime.LocalDate

data class DiseaseAlertSummary(
    val alertId: Long,
    val diseaseId: Long,
    val diseaseName: String,
    val blockId: Long?,
    val blockName: String?,
    val createdAt: LocalDate,
    val alert: String,
    val status: AlertStatus,
    val severity: AlertSeverity,
    val phenologicalStage: PhenologicalStage
)