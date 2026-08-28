package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(tableName = "vineyard", foreignKeys = [ForeignKey(
    entity = UserEntity::class,
    parentColumns = ["user_id"],
    childColumns = ["user_id"],
    onDelete = ForeignKey.CASCADE
)], indices = [
    Index("user_id")
])
class VineyardEntity(
    @PrimaryKey
    @ColumnInfo(name = "vineyard_id")
    val vineyardId: Long,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    val name: String,
    @ColumnInfo(name = "location_id")
    val locationId: Int,
    val size: Double,
    val country: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "time_zone")
    val timeZone: String,
    val elevation: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDate,
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDate
)