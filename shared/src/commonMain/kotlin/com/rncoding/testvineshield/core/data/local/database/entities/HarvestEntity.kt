package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(tableName = "harvest", foreignKeys = [ForeignKey(
    entity = BlockEntity::class,
    parentColumns = ["block_id"],
    childColumns = ["block_id"],
    onDelete = ForeignKey.CASCADE
)], indices = [
    Index("block_id"),
    Index(value = ["block_id", "harvest_date"])
]) // Add indices (blockId, date) or (blockId, harvestDate)
data class HarvestEntity(
    @PrimaryKey
    @ColumnInfo(name = "harvest_id")
    val harvestId: Long,
    @ColumnInfo(name = "block_id")
    val blockId: Long, //foreign key
    val season: String,
    @ColumnInfo(name = "harvest_date")
    val harvestDate: LocalDate,
    val units: String,
    val quantity: Int,
    @ColumnInfo(name = "brix_sugar_content")
    val brixSugarContent: Int
)
