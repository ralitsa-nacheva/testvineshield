package com.rncoding.testvineshield.core.data.local.database.projections

import androidx.room.ColumnInfo
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import kotlinx.datetime.LocalDate

data class DiseaseRiskSymptomProjection(
    @ColumnInfo(name = "symptom_id")
    val symptomId: Long,

    val code: String?,

    val name: String,

    @ColumnInfo(name = "plant_part")
    val plantPart: String,

    @ColumnInfo(name = "phenological_stage")
    val phenologicalStage: PhenologicalStage,

    @ColumnInfo(name = "observed_at")
    val observedAt: LocalDate,

    val severity: String,

    val notes: String
)