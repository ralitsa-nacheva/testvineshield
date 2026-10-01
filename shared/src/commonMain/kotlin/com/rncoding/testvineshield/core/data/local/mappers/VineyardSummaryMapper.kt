package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.projections.VineyardSummaryProjection
import com.rncoding.testvineshield.core.domain.datamodels.VineyardSummary

class VineyardSummaryMapper {

    fun projectionToDomain(
        projection: VineyardSummaryProjection
    ): VineyardSummary {

        return VineyardSummary(
            vineyardId = projection.vineyardId,
            name = projection.name,
            country = projection.country,
            city = projection.city,
            blockCount = projection.blockCount,
            lastActivityPriority =
                projection.lastActivityPriority,
            lastActivityStatus =
                projection.lastActivityStatus,
            latestTemperature =
                projection.latestTemperature,
            activeAlert =
                projection.activeAlert,
            currentDiseaseCount =
                projection.currentDiseaseCount,
            sortOrder =
                projection.sortOrder
        )
    }
}