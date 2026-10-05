package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseOccurrenceDomainModel

class DiseaseOccurrenceMapper {

    fun toDomain(
        entity: DiseaseOccurrenceEntity
    ): DiseaseOccurrenceDomainModel {
        return DiseaseOccurrenceDomainModel(
            occurrenceId = entity.occurrenceId,
            diseaseId = entity.diseaseId,
            vineyardId = entity.vineyardId,
            blockId = entity.blockId,
            observedAt = entity.observedAt,
            severity = entity.severity,
            status = entity.status,
            curedAt = entity.curedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            notes = entity.notes
        )
    }

    fun toEntity(
        domain: DiseaseOccurrenceDomainModel
    ): DiseaseOccurrenceEntity {
        return DiseaseOccurrenceEntity(
            occurrenceId = domain.occurrenceId,
            diseaseId = domain.diseaseId,
            vineyardId = domain.vineyardId,
            blockId = domain.blockId,
            observedAt = domain.observedAt,
            severity = domain.severity,
            status = domain.status,
            curedAt = domain.curedAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            notes = domain.notes
        )
    }
}