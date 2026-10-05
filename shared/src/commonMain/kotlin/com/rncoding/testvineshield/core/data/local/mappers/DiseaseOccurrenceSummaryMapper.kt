package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.projections.DiseaseOccurrenceSummaryProjection
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseOccurrenceSummary

class DiseaseOccurrenceSummaryMapper {

    fun toDomain(
        projection: DiseaseOccurrenceSummaryProjection
    ): DiseaseOccurrenceSummary {
        return DiseaseOccurrenceSummary(
            occurrenceId = projection.occurrenceId,
            diseaseId = projection.diseaseId,
            diseaseName = projection.diseaseName,
            blockId = projection.blockId,
            blockName = projection.blockName,
            observedAt = projection.observedAt,
            severity = projection.severity,
            status = projection.status
        )
    }
}