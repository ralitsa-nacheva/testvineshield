package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.DiseaseAlertDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseOccurrenceDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.SymptomDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import kotlinx.datetime.LocalDate
import com.rncoding.testvineshield.core.domain.datamodels.CompletedBlockActivity
import com.rncoding.testvineshield.core.domain.datamodels.LinkedObservationActivity
import com.rncoding.testvineshield.core.domain.datamodels.ReferenceSymptom
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.domain.datamodels.BlockDiseaseObservation

interface DiseaseRepository {
    fun observeOwnedBlockOccurrences(
        ownerId: Long, vineyardId: Long, blockId: Long
    ): Flow<List<BlockDiseaseObservation>>

    suspend fun createOwnedBlockOccurrence(ownerId: Long, occurrence: DiseaseOccurrenceDomainModel): Long?

    suspend fun updateOwnedBlockOccurrenceStatus(
        ownerId: Long,
        vineyardId: Long,
        blockId: Long,
        occurrenceId: Long,
        expectedUpdatedAt: Long,
        status: DiseaseOccurrenceStatus,
        curedAt: LocalDate?,
        updatedAt: Long
    ): Boolean

    suspend fun updateOwnedBlockOccurrenceDetails(
        ownerId: Long,
        vineyardId: Long,
        blockId: Long,
        occurrenceId: Long,
        expectedUpdatedAt: Long,
        observedOn: LocalDate,
        severity: Int,
        notes: String?,
        updatedAt: Long
    ): Boolean

    suspend fun recordOwnedReferenceSymptom(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long,
        expectedUpdatedAt: Long, observedOn: LocalDate, notes: String, symptom: ReferenceSymptom
    ): Boolean

    fun observeCompletedActivitiesForLink(
        ownerId: Long, vineyardId: Long, blockId: Long
    ): Flow<List<CompletedBlockActivity>>

    fun observeOwnedOccurrenceActivityLinks(
        ownerId: Long, vineyardId: Long, blockId: Long
    ): Flow<List<LinkedObservationActivity>>

    suspend fun linkOwnedCompletedActivity(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long,
        expectedUpdatedAt: Long, activityId: Long, completedOn: LocalDate, linkedOn: LocalDate
    ): Boolean

    suspend fun unlinkOwnedOccurrenceActivity(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long, activityId: Long
    ): Boolean

    suspend fun updateOwnedReferenceSymptom(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long,
        expectedUpdatedAt: Long, symptomId: Long, previousDate: LocalDate, previousNotes: String,
        observedOn: LocalDate, notes: String
    ): Boolean

    suspend fun removeOwnedReferenceSymptom(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long,
        expectedUpdatedAt: Long, symptomId: Long, previousDate: LocalDate, previousNotes: String
    ): Boolean

    fun observePredefinedDiseases(): Flow<List<DiseaseDomainModel>>


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

    suspend fun deleteOwnedBlockOccurrence(
        ownerId: Long, vineyardId: Long, blockId: Long, occurrenceId: Long, expectedUpdatedAt: Long
    ): Boolean

    suspend fun deleteDiseaseOccurrence(
        occurrenceId: Long
    )


    suspend fun getDisease(
        diseaseId: Long
    ): DiseaseDomainModel?

    suspend fun createDisease(
        disease: DiseaseDomainModel
    ): Long

    suspend fun updateDisease(
        disease: DiseaseDomainModel
    ): Boolean

    suspend fun deleteDisease(
        diseaseId: Long
    ): Boolean

    suspend fun isDiseaseReferenced(diseaseId: Long): Boolean

}