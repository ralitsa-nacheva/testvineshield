package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(tableName = "disease_treatment", foreignKeys = [ForeignKey(
    entity = ActivityEntity::class,
    parentColumns = ["activity_id"],
    childColumns = ["activity_id"],
    onDelete = ForeignKey.CASCADE
),
    ForeignKey(
        entity = DiseaseOccurrenceEntity::class,
        parentColumns = ["occurrence_id"],
        childColumns = ["occurrence_id"],
        onDelete = ForeignKey.CASCADE
    )], indices = [

    Index("occurrence_id"),
    Index("activity_id"),
    //Index(value = ["occurrence_id", "applied_at"])
]) //Add indices (activityId, performedAt) or (occurrenceId, appliedAt)
data class DiseaseTreatmentEntity(
    @PrimaryKey
    @ColumnInfo(name = "treatment_id")
    val treatmentId: Long,
    @ColumnInfo(name = "activity_id")
    val activityId: Long,
    @ColumnInfo(name = "occurrence_id")
    val occurrenceId: Long,
    @ColumnInfo(name = "treatment_product")
    val treatmentProduct: String?, // name would be more appropriate?
    val units: String?,
    @ColumnInfo(name = "treatment_product_quantity")
    val treatmentProductQuantity: Int?,
    @ColumnInfo(name = "water_quantity")
    val waterQuantity: Int?,
    @ColumnInfo(name = "treatment_product_rate")
    val treatmentProductRate: String?
)
