package com.rncoding.testvineshield.core.domain.disease_risk

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
interface DiseaseRiskMonitoringConfigurationRepository {
    suspend fun saveBotrytisDraft(
        ownerId: Long, vineyardId: Long, blockId: Long, startDate: LocalDate
    ): MonitoringDraftSaveOutcome

    /** All saved configurations, including disabled records. No model execution. */
    fun observeConfigurationsForVineyard(vineyardId: Long): Flow<List<DiseaseRiskMonitoringConfiguration>>


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
