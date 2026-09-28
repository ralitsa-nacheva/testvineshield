package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(tableName = "block", foreignKeys = [ForeignKey(
    entity = VineyardEntity::class,
    parentColumns = ["vineyard_id"],
    childColumns = ["vineyard_id"],
    onDelete = ForeignKey.CASCADE
)], indices = [Index("vineyard_id")])
data class BlockEntity(
    @PrimaryKey
    @ColumnInfo(name = "block_id")
    val blockId: Long,
    @ColumnInfo(name = "vineyard_id")
    val vineyardId: Long, // foreign key
    val name: String,
    val area: Int,
    @ColumnInfo(name = "vine_variety")
    val vineVariety: String,
    val color: String,
    @ColumnInfo(name = "root_stock")
    val rootStock: String,
    val rows: String,
    @ColumnInfo(name = "row_spacing")
    val rowSpacing: Int,
    @ColumnInfo(name = "planted_at")
    val plantedAt: LocalDate

)