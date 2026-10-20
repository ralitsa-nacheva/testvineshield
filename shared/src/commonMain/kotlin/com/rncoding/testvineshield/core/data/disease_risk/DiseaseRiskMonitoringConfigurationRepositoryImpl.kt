package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseRiskMonitoringConfigurationDao
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEvaluationCoordinator
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelType
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskMonitoringConfiguration
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskMonitoringConfigurationRepository

class DiseaseRiskMonitoringConfigurationRepositoryImpl(
    private val dao: DiseaseRiskMonitoringConfigurationDao,
    private val coordinator: DiseaseRiskEvaluationCoordinator
) : DiseaseRiskMonitoringConfigurationRepository {

    override suspend fun getConfiguration(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        modelType: DiseaseRiskModelType
    ): DiseaseRiskMonitoringConfiguration? {

        val entity = dao.getConfiguration(
            vineyardId = vineyardId,
            blockId = blockId,
            diseaseId = diseaseId,
            modelType = modelType.name
        )

        return entity?.let(
            DiseaseRiskMonitoringConfigurationMapper::toDomain
        )
    }

    override suspend fun saveConfiguration(
        configuration: DiseaseRiskMonitoringConfiguration
    ) {
        coordinator.withExclusiveEvaluation {

            val existing = dao.getConfiguration(
                vineyardId = configuration.vineyardId,
                blockId = configuration.blockId,
                diseaseId = configuration.diseaseId,
                modelType = configuration.modelType.name
            )

            val newEntity =
                DiseaseRiskMonitoringConfigurationMapper.toEntity(
                    configuration
                )

            when {
                existing == null -> {
                    dao.saveConfigurationAndResetState(newEntity)
                }

                existing.monitoringStartDate !=
                        newEntity.monitoringStartDate -> {
                    dao.saveConfigurationAndResetState(newEntity)
                }

                else -> {
                    dao.upsertConfiguration(newEntity)
                }
            }
        }
    }

    override suspend fun deleteConfiguration(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        modelType: DiseaseRiskModelType
    ) {
        coordinator.withExclusiveEvaluation {

            dao.deleteConfigurationAndState(
                vineyardId = vineyardId,
                blockId = blockId,
                diseaseId = diseaseId,
                modelType = modelType.name
            )
        }
    }


    override suspend fun getEnabledConfigurationsForVineyard(
        vineyardId: Long
    ): List<DiseaseRiskMonitoringConfiguration> {
        return dao.getEnabledConfigurationsForVineyard(vineyardId)
            .map(DiseaseRiskMonitoringConfigurationMapper::toDomain)
    }

}