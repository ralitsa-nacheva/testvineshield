package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["disease_id", "symptom_id"])
data class DiseaseSymptomCrossRef(
    @ColumnInfo(name = "disease_id")
    val diseaseId: Long,
    @ColumnInfo(name = "symptom_id")
    val symptomId: Long

)