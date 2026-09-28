package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.datetime.LocalDate

@Entity(primaryKeys = ["occurrence_id", "symptom_id"], foreignKeys = [ForeignKey(
    entity = DiseaseOccurrenceEntity::class,
    parentColumns = ["occurrence_id"],
    childColumns = ["occurrence_id"],
    onDelete = ForeignKey.CASCADE
), ForeignKey(
    entity = SymptomEntity::class,
    parentColumns = ["symptom_id"],
    childColumns = ["symptom_id"],
    onDelete = ForeignKey.RESTRICT
)], indices = [
    Index("symptom_id"),
    Index(value = ["occurrence_id", "observed_at"])
]) // Add indices (occurrenceId, observedAt) or (occurrenceId, observedAt)
data class OccurrenceSymptomCrossRef(
    @ColumnInfo(name = "occurrence_id")
    val occurrenceId: Long,
    @ColumnInfo(name = "symptom_id")
    val symptomId: Long,
    @ColumnInfo(name = "observed_at")
    val observedAt: LocalDate,
    val severity: String,
    val notes: String
)