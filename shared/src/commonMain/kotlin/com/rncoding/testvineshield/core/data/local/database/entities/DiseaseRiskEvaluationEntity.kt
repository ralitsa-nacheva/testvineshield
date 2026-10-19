package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "disease_risk_evaluation",
    foreignKeys = [
        ForeignKey(
            entity = DiseaseEntity::class,
            parentColumns = ["disease_id"],
            childColumns = ["disease_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VineyardEntity::class,
            parentColumns = ["vineyard_id"],
            childColumns = ["vineyard_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BlockEntity::class,
            parentColumns = ["block_id"],
            childColumns = ["block_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("disease_id"),
        Index("vineyard_id"),
        Index("block_id"),
        Index(
            value = [
                "vineyard_id",
                "block_id",
                "disease_id",
                "model_type",
                "calculated_at"
            ]
        )
    ]
)
data class DiseaseRiskEvaluationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "evaluation_id")
    val evaluationId: Long = 0L,

    @ColumnInfo(name = "disease_id")
    val diseaseId: Long,

    @ColumnInfo(name = "vineyard_id")
    val vineyardId: Long,

    @ColumnInfo(name = "block_id")
    val blockId: Long,

    @ColumnInfo(name = "calculated_at")
    val calculatedAt: Long,

    @ColumnInfo(name = "model_type")
    val modelType: String,

    @ColumnInfo(name = "model_version")
    val modelVersion: String,

    @ColumnInfo(name = "evaluation_status")
    val evaluationStatus: String,

    @ColumnInfo(name = "risk_level")
    val riskLevel: String,

    val confidence: String,

    val event: String?,

    @ColumnInfo(name = "native_risk_value")
    val nativeRiskValue: Double?
)