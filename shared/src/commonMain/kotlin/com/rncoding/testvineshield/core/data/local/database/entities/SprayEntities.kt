package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.*
import kotlinx.datetime.LocalDate

@Entity(tableName = "spray_application", foreignKeys = [ForeignKey(entity = ActivityEntity::class,
    parentColumns = ["activity_id"], childColumns = ["activity_id"], onDelete = ForeignKey.CASCADE)])
data class SprayApplicationEntity(
    @PrimaryKey @ColumnInfo(name = "activity_id") val activityId: Long,
    @ColumnInfo(name = "application_date") val applicationDate: LocalDate,
    @ColumnInfo(name = "local_time") val localTime: String?,
    @ColumnInfo(name = "time_zone") val timeZone: String,
    @ColumnInfo(name = "treated_area") val treatedArea: Double?,
    @ColumnInfo(name = "area_unit") val areaUnit: String?,
    @ColumnInfo(name = "water_quantity") val waterQuantity: Double?,
    @ColumnInfo(name = "water_unit") val waterUnit: String?,
    val notes: String?,
    val revision: Long,
    val generation: String
)

@Entity(tableName = "spray_product", foreignKeys = [ForeignKey(entity = SprayApplicationEntity::class,
    parentColumns = ["activity_id"], childColumns = ["activity_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["activity_id", "line_position"], unique = true)])
data class SprayProductEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "product_line_id") val lineId: Long,
    @ColumnInfo(name = "activity_id") val activityId: Long,
    @ColumnInfo(name = "line_position") val position: Int,
    @ColumnInfo(name = "product_name") val productName: String,
    val quantity: Double?,
    @ColumnInfo(name = "quantity_unit") val quantityUnit: String?,
    @ColumnInfo(name = "label_reference") val labelReference: String?,
    @ColumnInfo(name = "rate_quantity") val rateQuantity: Double? = null,
    @ColumnInfo(name = "rate_product_unit") val rateProductUnit: String? = null,
    @ColumnInfo(name = "rate_basis_quantity") val rateBasisQuantity: Double? = null,
    @ColumnInfo(name = "rate_basis_unit") val rateBasisUnit: String? = null
)
