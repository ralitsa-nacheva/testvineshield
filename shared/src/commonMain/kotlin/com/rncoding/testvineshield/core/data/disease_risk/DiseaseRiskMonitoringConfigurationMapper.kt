package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseRiskMonitoringConfigurationEntity
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelType
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskMonitoringConfiguration
import kotlinx.datetime.LocalDate

object DiseaseRiskMonitoringConfigurationMapper {

    fun toEntity(
        configuration: DiseaseRiskMonitoringConfiguration
    ): DiseaseRiskMonitoringConfigurationEntity {

        return DiseaseRiskMonitoringConfigurationEntity(
            vineyardId = configuration.vineyardId,
            blockId = configuration.blockId,
            diseaseId = configuration.diseaseId,
            modelType = configuration.modelType.name,
            monitoringStartDate = configuration.monitoringStartDate.toString(),
            enabled = configuration.enabled
        )
    }

    fun toDomain(
        entity: DiseaseRiskMonitoringConfigurationEntity
    ): DiseaseRiskMonitoringConfiguration {

        return DiseaseRiskMonitoringConfiguration(
            vineyardId = entity.vineyardId,
            blockId = entity.blockId,
            diseaseId = entity.diseaseId,
            modelType = DiseaseRiskModelType.valueOf(entity.modelType),
            monitoringStartDate = LocalDate.parse(entity.monitoringStartDate),
            enabled = entity.enabled
        )
    }
}