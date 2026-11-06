package com.rncoding.testvineshield.core.data.local.database.entities

import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
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
data class VineyardEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "vineyard_id")
    val vineyardId: Long=0L,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    val name: String,
    val size: Double,
    val country: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "time_zone")
    val timeZone: String,
    val elevation: Double?,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDate,
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDate,
    @ColumnInfo(name = "size_unit", defaultValue = "NULL")
    val sizeUnit: AreaUnit? = null
)