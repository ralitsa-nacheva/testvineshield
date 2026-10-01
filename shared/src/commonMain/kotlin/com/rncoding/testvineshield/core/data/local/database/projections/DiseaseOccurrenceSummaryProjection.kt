package com.rncoding.testvineshield.core.data.local.database.projections

import androidx.room.ColumnInfo
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import kotlinx.datetime.LocalDate

data class DiseaseOccurrenceSummaryProjection(
    @ColumnInfo(name = "occurrence_id")
    val occurrenceId: Long,

    @ColumnInfo(name = "disease_id")
    val diseaseId: Long,

    @ColumnInfo(name = "disease_name")
    val diseaseName: String,

    @ColumnInfo(name = "block_id")
    val blockId: Long?,

    @ColumnInfo(name = "block_name")
    val blockName: String?,

    @ColumnInfo(name = "observed_at")
    val observedAt: LocalDate,

    val severity: Int,

    val status: DiseaseOccurrenceStatus
)