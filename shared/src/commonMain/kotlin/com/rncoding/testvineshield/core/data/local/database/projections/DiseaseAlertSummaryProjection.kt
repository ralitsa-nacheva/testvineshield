package com.rncoding.testvineshield.core.data.local.database.projections

import androidx.room.ColumnInfo
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertSeverity
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import kotlinx.datetime.LocalDate

data class DiseaseAlertSummaryProjection(
    @ColumnInfo(name = "alert_id")
    val alertId: Long,

    @ColumnInfo(name = "disease_id")
    val diseaseId: Long,

    @ColumnInfo(name = "disease_name")
    val diseaseName: String,

    @ColumnInfo(name = "block_id")
    val blockId: Long?,

    @ColumnInfo(name = "block_name")
    val blockName: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDate,

    val alert: String,

    @ColumnInfo(name = "alert_status")
    val status: AlertStatus,

    @ColumnInfo(name = "alert_severity")
    val severity: AlertSeverity,

    @ColumnInfo(name = "phenological_stage")
    val phenologicalStage: PhenologicalStage
)