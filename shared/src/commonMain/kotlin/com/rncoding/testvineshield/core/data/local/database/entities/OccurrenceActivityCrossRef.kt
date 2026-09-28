package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.datetime.LocalDate

@Entity(primaryKeys = ["occurrence_id", "activity_id" ], foreignKeys = [ForeignKey(
    entity = DiseaseOccurrenceEntity::class,
    parentColumns = ["occurrence_id"],
    childColumns = ["occurrence_id"],
    onDelete = ForeignKey.CASCADE
), ForeignKey(
    entity = ActivityEntity::class,
    parentColumns = ["activity_id"],
    childColumns = ["activity_id"],
    onDelete = ForeignKey.CASCADE
)], indices = [
    Index("occurrence_id"),
    Index("activity_id")
])
data class OccurrenceActivityCrossRef(
    @ColumnInfo(name = "occurrence_id")
    val occurrenceId: Long,
    @ColumnInfo(name = "activity_id")
    val activityId: Long,
    @ColumnInfo(name = "applied_at")
    val appliedAt: LocalDate,
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDate,
    val notes: String
)
