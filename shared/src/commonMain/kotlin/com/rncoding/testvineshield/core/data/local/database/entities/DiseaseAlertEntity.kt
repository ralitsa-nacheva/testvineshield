package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(tableName = "disease_alert", foreignKeys = [ForeignKey(
    entity = DiseaseEntity::class,
    parentColumns = ["disease_id"],
    childColumns = ["disease_id"],
    onDelete = ForeignKey.CASCADE
), ForeignKey(
    entity = VineyardEntity::class,
    parentColumns = ["vineyard_id"],
    childColumns = ["vineyard_id"],
    onDelete = ForeignKey.CASCADE
), ForeignKey(
    entity = BlockEntity::class,
    parentColumns = ["block_id"],
    childColumns = ["block_id"],
    onDelete = ForeignKey.CASCADE
)], indices = [
    Index("disease_id"),
    Index("vineyard_id"),
    Index("block_id"),
    Index(value = ["vineyard_id", "alert_status", "created_at"])
]) // Add indices (vineyardId, isOpen, createdAt) or (vineyardId, isOpen, createdAt) and (blockId)
data class DiseaseAlertEntity(
    @PrimaryKey
    @ColumnInfo(name = "alert_id")
    val alertId: Long,
    @ColumnInfo(name = "disease_id")
    val diseaseId: Long,
    @ColumnInfo(name = "vineyard_id")
    val vineyardId: Long,
    @ColumnInfo(name = "block_id")
    val blockId: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDate,
    val alert: String,
    @ColumnInfo(name = "alert_status")
    val alertStatus: String,
    @ColumnInfo(name = "alert_severity")
    val alertSeverity: String,
    @ColumnInfo(name = "phenological_stage")
    val phenologicalStage: String
)
