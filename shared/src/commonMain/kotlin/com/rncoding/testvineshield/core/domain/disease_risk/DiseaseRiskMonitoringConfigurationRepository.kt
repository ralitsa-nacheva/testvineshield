package com.rncoding.testvineshield.core.domain.disease_risk

interface DiseaseRiskMonitoringConfigurationRepository {

    suspend fun getConfiguration(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        modelType: DiseaseRiskModelType
    ): DiseaseRiskMonitoringConfiguration?

    suspend fun saveConfiguration(
        configuration: DiseaseRiskMonitoringConfiguration
    )

    suspend fun deleteConfiguration(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        modelType: DiseaseRiskModelType
    )


    suspend fun getEnabledConfigurationsForVineyard(
        vineyardId: Long
    ): List<DiseaseRiskMonitoringConfiguration>

}