package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus


@Entity(tableName = "disease_occurrence", foreignKeys = [ForeignKey(
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
    Index(value = ["block_id", "observed_at"]),
    Index(value = ["vineyard_id", "status", "observed_at"])
])
data class DiseaseOccurrenceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "occurrence_id")
    val occurrenceId: Long,
    @ColumnInfo(name = "disease_id")
    val diseaseId: Long,
    @ColumnInfo(name = "vineyard_id")
    val vineyardId: Long,
    @ColumnInfo(name = "block_id")
    val blockId: Long?,
    @ColumnInfo(name = "observed_at")
    val observedAt: LocalDate,
    // Severity is the percentage of affected tissue/area: 0..100.
    // Enforced by the domain validation layer.
    val severity: Int,
    val status: DiseaseOccurrenceStatus,
    @ColumnInfo(name = "cured_at")
    val curedAt: LocalDate?,
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDate,
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDate,
    val notes: String?
)
