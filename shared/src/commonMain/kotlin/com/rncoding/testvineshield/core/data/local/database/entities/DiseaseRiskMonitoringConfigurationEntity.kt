package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "disease_risk_monitoring_configuration",
    primaryKeys = [
        "vineyard_id",
        "block_id",
        "disease_id",
        "model_type"
    ],
    foreignKeys = [
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
        ),
        ForeignKey(
            entity = DiseaseEntity::class,
            parentColumns = ["disease_id"],
            childColumns = ["disease_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("block_id"),
        Index("disease_id")
    ]
)
data class DiseaseRiskMonitoringConfigurationEntity(

    @ColumnInfo(name = "vineyard_id")
    val vineyardId: Long,

    @ColumnInfo(name = "block_id")
    val blockId: Long,

    @ColumnInfo(name = "disease_id")
    val diseaseId: Long,

    @ColumnInfo(name = "model_type")
    val modelType: String,

    @ColumnInfo(name = "monitoring_start_date")
    val monitoringStartDate: String,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean
)