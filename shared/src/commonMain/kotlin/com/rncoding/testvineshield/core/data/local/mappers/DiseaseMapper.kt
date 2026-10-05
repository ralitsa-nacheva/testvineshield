package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel

class DiseaseMapper {

    fun toDomain(
        entity: DiseaseEntity
    ): DiseaseDomainModel {
        return DiseaseDomainModel(
            diseaseId = entity.diseaseId,
            code = entity.code,
            name = entity.name,
            description = entity.description,
            vectors = entity.vectors,
            factors = entity.factors,
            treatmentSuggestion = entity.treatmentSuggestion
        )
    }

    fun toEntity(
        domain: DiseaseDomainModel
    ): DiseaseEntity {
        return DiseaseEntity(
            diseaseId = domain.diseaseId,
            code = domain.code,
            name = domain.name,
            description = domain.description,
            vectors = domain.vectors,
            factors = domain.factors,
            treatmentSuggestion = domain.treatmentSuggestion
        )
    }
}