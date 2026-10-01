package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage

data class BlockSummary(
    val blockId: Long,
    val name: String,
    val area: Int,
    val vineVariety: String,
    val phenologicalStage: PhenologicalStage?,
    val latestActivityType: String?,
    val latestActivityPriority: ActivityPriority?,
    val latestActivityStatus: ActivityStatus?,
    val activeDiseaseCount: Int,
    val activeAlertCount: Int
)