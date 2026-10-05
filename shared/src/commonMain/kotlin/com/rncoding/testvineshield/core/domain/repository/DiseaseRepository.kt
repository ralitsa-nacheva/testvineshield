package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.DiseaseAlertDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseOccurrenceDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.SymptomDomainModel
import kotlinx.coroutines.flow.Flow

interface DiseaseRepository {

    fun observeDiseaseCatalog():
            Flow<List<DiseaseDomainModel>>

    fun observeSymptoms():
            Flow<List<SymptomDomainModel>>

    fun observeDiseaseOccurrences(
        blockId: Long
    ): Flow<List<DiseaseOccurrenceDomainModel>>

    fun observeActiveAlerts(
        vineyardId: Long
    ): Flow<List<DiseaseAlertDomainModel>>

    suspend fun getDiseaseOccurrence(
        occurrenceId: Long
    ): DiseaseOccurrenceDomainModel?

    suspend fun deleteDiseaseOccurrence(
        occurrenceId: Long
    )
}