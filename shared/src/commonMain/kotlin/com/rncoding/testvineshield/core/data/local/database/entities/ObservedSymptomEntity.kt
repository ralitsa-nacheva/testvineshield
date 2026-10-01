package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "observed_symptom",
    foreignKeys = [
        ForeignKey(
            entity = DiseaseOccurrenceEntity::class,
            parentColumns = ["occurrence_id"],
            childColumns = ["occurrence_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SymptomEntity::class,
            parentColumns = ["symptom_id"],
            childColumns = ["symptom_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("occurrence_id"),
        Index("symptom_id")
    ]
)
data class ObservedSymptomEntity
(   @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "observation_id")
    val observationId: Long,
    @ColumnInfo(name = "symptom_id")
    val symptomId: Long,
    @ColumnInfo(name = "occurrence_id")
    val occurrenceId: Long,
    @ColumnInfo(name = "observed_at")
    val observedAt: Long,
    val severity: Int,
    val note: String
)
