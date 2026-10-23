package com.rncoding.testvineshield.core.domain.datamodels

import androidx.room.ColumnInfo
import kotlinx.datetime.LocalDate

data class ActivityDomainModel(
    val activityId: Long,
    val vineyardId: Long,
    val blockId: Long?, // activity can be for the whole vineyard, not for separate block
    val activityType: String,
    val activityDescription: String,
    val season: String,
    val phenologicalStage: String, // enum?
    val priority: String,
    val status: String,
    val plannedAt: LocalDate,
    val completedAt: LocalDate?,
    val tool: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val treatmentProduct: String?,
    val treatmentProductQuantity: Double?,
    val treatmentProductUnits: String?,
    val waterQuantity: Double?,
    val treatmentProductRate: Double?
)
