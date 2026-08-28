package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "observed_symptom")
data class ObservedSymptomEntity
(   @PrimaryKey
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
