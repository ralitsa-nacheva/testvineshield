package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseAlertEntity
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseAlertDomainModel

class DiseaseAlertMapper {

    fun toDomain(
        entity: DiseaseAlertEntity
    ): DiseaseAlertDomainModel {
        return DiseaseAlertDomainModel(
            alertId = entity.alertId,
            diseaseId = entity.diseaseId,
            vineyardId = entity.vineyardId,
            blockId = entity.blockId,
            createdAt = entity.createdAt,
            alert = entity.alert,
            alertStatus = entity.alertStatus,
            alertSeverity = entity.alertSeverity,
            phenologicalStage = entity.phenologicalStage
        )
    }

    fun toEntity(
        domain: DiseaseAlertDomainModel
    ): DiseaseAlertEntity {
        return DiseaseAlertEntity(
            alertId = domain.alertId,
            diseaseId = domain.diseaseId,
            vineyardId = domain.vineyardId,
            blockId = domain.blockId,
            createdAt = domain.createdAt,
            alert = domain.alert,
            alertStatus = domain.alertStatus,
            alertSeverity = domain.alertSeverity,
            phenologicalStage = domain.phenologicalStage
        )
    }
}