package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "disease_risk_model_state",
    primaryKeys = [
        "vineyard_id",
        "block_id",
        "disease_id",
        "model_type"
    ],
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
        Index("block_id")
    ]
)
data class DiseaseRiskModelStateEntity(
    @ColumnInfo(name = "vineyard_id")
    val vineyardId: Long,

    @ColumnInfo(name = "block_id")
    val blockId: Long,

    @ColumnInfo(name = "disease_id")
    val diseaseId: Long,

    @ColumnInfo(name = "model_type")
    val modelType: String,

    @ColumnInfo(name = "model_version")
    val modelVersion: String,

    @ColumnInfo(name = "state_type")
    val stateType: String,

    @ColumnInfo(name = "state_payload")
    val statePayload: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)