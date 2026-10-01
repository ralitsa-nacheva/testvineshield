package com.rncoding.testvineshield.core.data.local.database.projections

import androidx.room.ColumnInfo
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage

data class BlockSummaryProjection(
    @ColumnInfo(name = "block_id")
    val blockId: Long,

    val name: String,

    val area: Int,

    @ColumnInfo(name = "vine_variety")
    val vineVariety: String,

    @ColumnInfo(name = "phenological_stage")
    val phenologicalStage: PhenologicalStage?,

    @ColumnInfo(name = "latest_activity_type")
    val latestActivityType: String?,

    @ColumnInfo(name = "latest_activity_priority")
    val latestActivityPriority: ActivityPriority?,

    @ColumnInfo(name = "latest_activity_status")
    val latestActivityStatus: ActivityStatus?,

    @ColumnInfo(name = "active_disease_count")
    val activeDiseaseCount: Int,

    @ColumnInfo(name = "active_alert_count")
    val activeAlertCount: Int
)