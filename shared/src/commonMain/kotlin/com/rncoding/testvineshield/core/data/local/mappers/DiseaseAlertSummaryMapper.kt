package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.projections.DiseaseAlertSummaryProjection
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseAlertSummary

class DiseaseAlertSummaryMapper {

    fun toDomain(
        projection: DiseaseAlertSummaryProjection
    ): DiseaseAlertSummary {
        return DiseaseAlertSummary(
            alertId = projection.alertId,
            diseaseId = projection.diseaseId,
            diseaseName = projection.diseaseName,
            blockId = projection.blockId,
            blockName = projection.blockName,
            createdAt = projection.createdAt,
            alert = projection.alert,
            status = projection.status,
            severity = projection.severity,
            phenologicalStage = projection.phenologicalStage
        )
    }
}