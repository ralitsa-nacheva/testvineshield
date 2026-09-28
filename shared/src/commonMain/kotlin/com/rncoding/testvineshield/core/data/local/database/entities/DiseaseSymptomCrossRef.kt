package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "disease_symptom_cross_ref",
    primaryKeys = ["disease_id", "symptom_id"],
    foreignKeys = [
        ForeignKey(
            entity = DiseaseEntity::class,
            parentColumns = ["disease_id"],
            childColumns = ["disease_id"],
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
        Index("symptom_id")
    ]
)

data class DiseaseSymptomCrossRef(
    @ColumnInfo(name = "disease_id")
    val diseaseId: Long,
    @ColumnInfo(name = "symptom_id")
    val symptomId: Long

)