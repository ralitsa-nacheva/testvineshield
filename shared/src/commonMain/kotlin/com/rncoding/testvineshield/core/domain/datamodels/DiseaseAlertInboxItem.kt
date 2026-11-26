package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.domain.datamodels.enums.*
import kotlinx.datetime.LocalDate

data class DiseaseAlertInboxItem(
    val alertId: Long,
    val vineyardId: Long,
    val vineyardName: String,
    val blockId: Long?,
    val blockName: String?,
    val diseaseId: Long,
    val diseaseCode: String?,
    val diseaseName: String,
    val createdAt: LocalDate,
    val message: String,
    val status: AlertStatus,
    val severity: AlertSeverity,
    val stage: PhenologicalStage
)
