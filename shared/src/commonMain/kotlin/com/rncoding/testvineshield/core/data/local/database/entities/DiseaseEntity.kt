package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "disease")
data class DiseaseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "disease_id")
    val diseaseId: Long,
    @ColumnInfo(name = "code")
    val code: String?,
    val name: String,
    val description: String,
    val vectors: String,
    val factors: String,
    @ColumnInfo(name = "treatment_suggestion")
    val treatmentSuggestion: String
)