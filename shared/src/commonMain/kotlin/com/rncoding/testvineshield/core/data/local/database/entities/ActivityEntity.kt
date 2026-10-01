package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage

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
])
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "activity_id")
    val activityId: Long,
    @ColumnInfo(name = "vineyard_id")
    val vineyardId: Long,
    @ColumnInfo(name = "block_id")
    val blockId: Long?, // activity can be for the whole vineyard, not for separate block
    @ColumnInfo(name = "activity_type")
    val activityType: String,
    @ColumnInfo(name = "activity_description")
    val activityDescription: String,
    val season: String,
    @ColumnInfo(name = "phenological_stage")
    val phenologicalStage: PhenologicalStage,
    val priority: ActivityPriority,
    val status: ActivityStatus,
    @ColumnInfo(name = "planned_at")
    val plannedAt: LocalDate,
    @ColumnInfo(name = "completed_at")
    val completedAt: LocalDate?,
    val tool: String?,
    val notes: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDate,
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDate,
    @ColumnInfo(name = "treatment_product")
    val treatmentProduct: String?,
    @ColumnInfo(name = "treatment_product_quantity")
    val treatmentProductQuantity: Double?,
    @ColumnInfo(name = "treatment_product_units")
    val treatmentProductUnits: String?,
    @ColumnInfo(name = "water_quantity")
    val waterQuantity: Double?,
    @ColumnInfo(name = "treatment_product_rate")
    val treatmentProductRate: Double?
)