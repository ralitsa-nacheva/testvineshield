package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.SymptomEntity
import com.rncoding.testvineshield.core.domain.datamodels.SymptomDomainModel

class SymptomMapper {

    fun toDomain(
        entity: SymptomEntity
    ): SymptomDomainModel {
        return SymptomDomainModel(
            symptomId = entity.symptomId,
            code = entity.code,
            name = entity.name,
            plantPart = entity.plantPart,
            phenologicalStage = entity.phenologicalStage,
            notes = entity.notes
        )
    }

    fun toEntity(
        domain: SymptomDomainModel
    ): SymptomEntity {
        return SymptomEntity(
            symptomId = domain.symptomId,
            code = domain.code,
            name = domain.name,
            plantPart = domain.plantPart,
            phenologicalStage = domain.phenologicalStage,
            notes = domain.notes
        )
    }
}