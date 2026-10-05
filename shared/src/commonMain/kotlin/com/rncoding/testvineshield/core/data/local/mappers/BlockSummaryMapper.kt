package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.projections.BlockSummaryProjection
import com.rncoding.testvineshield.core.domain.datamodels.BlockSummary

class BlockSummaryMapper {

    fun toDomain(
        projection: BlockSummaryProjection
    ): BlockSummary {
        return BlockSummary(
            blockId = projection.blockId,
            name = projection.name,
            area = projection.area,
            vineVariety = projection.vineVariety,
            phenologicalStage = projection.phenologicalStage,
            latestActivityType = projection.latestActivityType,
            latestActivityPriority = projection.latestActivityPriority,
            latestActivityStatus = projection.latestActivityStatus,
            activeDiseaseCount = projection.activeDiseaseCount,
            activeAlertCount = projection.activeAlertCount
        )
    }
}