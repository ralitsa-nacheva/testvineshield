package com.rncoding.testvineshield.core.domain.disease_risk

import kotlinx.datetime.LocalDate

interface DiseaseRiskHostContextProvider {

    suspend fun getHostContext(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        stageDate: LocalDate? = null
    ): DiseaseRiskHostContext
}