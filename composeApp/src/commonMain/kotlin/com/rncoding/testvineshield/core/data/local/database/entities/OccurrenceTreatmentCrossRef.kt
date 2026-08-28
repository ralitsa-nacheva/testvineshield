package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import kotlinx.datetime.LocalDate

@Entity(primaryKeys = ["occurrence_id", "treatment_id" ], foreignKeys = [ForeignKey(
    entity = DiseaseOccurrenceEntity::class,
    parentColumns = ["occurrence_id"],
    childColumns = ["occurrence_id"],
    onDelete = ForeignKey.CASCADE
), ForeignKey(
    entity = DiseaseTreatmentEntity::class,
    parentColumns = ["treatment_id"],
    childColumns = ["treatment_id"],
    onDelete = ForeignKey.SET_NULL
)])
data class OccurrenceTreatmentCrossRef(
    @ColumnInfo(name = "occurrence_id")
    val occurrenceId: Long,
    @ColumnInfo(name = "treatment_id")
    val treatmentId: Long,
    @ColumnInfo(name = "applied_at")
    val appliedAt: LocalDate,
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDate,
    val notes: String
)
