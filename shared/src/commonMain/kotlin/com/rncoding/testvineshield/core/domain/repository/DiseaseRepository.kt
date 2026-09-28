package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.DiseaseAlertDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseOccurrenceDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.SymptomDomainModel
import kotlinx.coroutines.flow.Flow

interface DiseaseRepository {
    fun observeDiseaseCatalog(): Flow<List<DiseaseDomainModel>>
    fun observeDiseaseOccurrences(
        blockId: Long
    ): Flow<List<DiseaseOccurrenceDomainModel>>//returns domain model

    fun observeSymptoms(): Flow<List<SymptomDomainModel>>//returns domain model
    fun observeActiveAlerts(vineyardId: Long): Flow<List<DiseaseAlertDomainModel>>//returns domain model
    fun observeDiseaseOccurrenceDetails(occurrenceId: Long): Flow<DiseaseOccurrenceDomainModel> // ??returns aggregate
    fun observeCaseSummariesForBlock(blockId: Long): Flow<List<DiseaseCaseSummary>>//returns domain model
    suspend fun createOccurrence(cmd: RecordDiseaseOccurrence): Long // to entity
    suspend fun addObservedSymptom(cmd: AddObservedSymptom) // to entity
    suspend fun removeObservedSymptom(observationId: Long) // to entity
    suspend fun addTreatment(cmd: AddTreatment) // to entity
    suspend fun removeTreatment(treatmentId: Long) // to entity
    suspend fun addSymptomAndTreatment(
        occurrenceId: Long,
        symptomId: Long,
        treatmentName: String
    ) // to entity
    suspend fun markCured(occurrenceId: Long, curedAt: Instant) // to entity
    suspend fun reopenCase(occurrenceId: Long) // to entity
    suspend fun createAlert(cmd: CreateAlert): Long // to entity
    suspend fun resolveAlert(alertId: Long, resolvedAt: Instant) // to entity
    //delete disease occurrence, get occurrence by id, delete all, get all
    suspend fun saveOccurrenceWithSymptoms()
}