package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "symptom")
data class SymptomEntity(
    @PrimaryKey
    @ColumnInfo(name = "symptom_id")
    val symptomId: Long,
    val name: String,
    @ColumnInfo(name = "plant_part")
    val plantPart: String,
    @ColumnInfo(name = "phenological_stage")
    val phenologicalStage: String,
    val notes: String
)