package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(tableName = "activity", foreignKeys = [ForeignKey(
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
    )], indices = [
    Index("vineyard_id"),
    Index("block_id"),
    Index(value = ["block_id", "completed_at"]),
    Index(value = ["vineyard_id", "completed_at"])
]) // TODO Add indices (blockId, performedAt) or (blockId, completedDate), (vineyardId, completedDate)
data class ActivityEntity(
    @PrimaryKey
    @ColumnInfo(name = "activity_id")
    val activityId: Long,
    @ColumnInfo(name = "vineyard_id")
    val vineyardId: Long,
    @ColumnInfo(name = "block_id")
    val blockId: Long?, // activity can be for the whole vineyard, not for separate block
    val activity: String,
    val season: String,
    @ColumnInfo(name = "phenological_stage")
    val phenologicalStage: String, // enum?
    val priority: String,
    val status: String,
    @ColumnInfo(name = "planned_at")
    val plannedAt: LocalDate,
    @ColumnInfo(name = "completed_at")
    val completedAt: LocalDate,
    val tool: String?,
    val notes: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDate,
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDate
)