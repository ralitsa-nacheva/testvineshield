package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseAlertDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseOccurrenceDao
import com.rncoding.testvineshield.core.data.local.database.daos.SymptomDao
import com.rncoding.testvineshield.core.data.local.mappers.DiseaseAlertMapper
import com.rncoding.testvineshield.core.data.local.mappers.DiseaseMapper
import com.rncoding.testvineshield.core.data.local.mappers.DiseaseOccurrenceMapper
import com.rncoding.testvineshield.core.data.local.mappers.SymptomMapper
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseAlertDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseOccurrenceDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.SymptomDomainModel
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.domain.datamodels.BlockDiseaseObservation
import kotlinx.coroutines.flow.map

class DiseaseRepositoryImpl(
    private val diseaseDao: DiseaseDao,
    private val diseaseOccurrenceDao: DiseaseOccurrenceDao,
    private val symptomDao: SymptomDao,
    private val diseaseAlertDao: DiseaseAlertDao,
    private val diseaseMapper: DiseaseMapper,
    private val diseaseOccurrenceMapper: DiseaseOccurrenceMapper,
    private val symptomMapper: SymptomMapper,
    private val diseaseAlertMapper: DiseaseAlertMapper
) : DiseaseRepository {
    override fun observeOwnedBlockOccurrences(
        ownerId: Long, vineyardId: Long, blockId: Long
    ): Flow<List<BlockDiseaseObservation>> =
        diseaseOccurrenceDao.observeOwnedBlockOccurrences(ownerId, vineyardId, blockId).map { rows ->
            rows.map { BlockDiseaseObservation(it.id, it.diseaseName, it.observedOn, it.severity, it.status, it.notes, it.curedAt, it.updatedAt) }
        }

    override suspend fun createOwnedBlockOccurrence(ownerId: Long, occurrence: DiseaseOccurrenceDomainModel): Long? =
        diseaseOccurrenceDao.insertOwnedBlockOccurrence(ownerId, diseaseOccurrenceMapper.toEntity(occurrence))

    override suspend fun updateOwnedBlockOccurrenceStatus(
        ownerId: Long,
        vineyardId: Long,
        blockId: Long,
        occurrenceId: Long,
        expectedUpdatedAt: Long,
        status: DiseaseOccurrenceStatus,
        curedAt: LocalDate?,
        updatedAt: Long
    ): Boolean = diseaseOccurrenceDao.updateOwnedBlockOccurrenceStatus(
        ownerId, vineyardId, blockId, occurrenceId, expectedUpdatedAt, status, curedAt, updatedAt
    ) == 1

    override fun observePredefinedDiseases(): Flow<List<DiseaseDomainModel>> =
        diseaseDao.observePredefinedDiseases().map { rows -> rows.map(diseaseMapper::toDomain) }


    override fun observeDiseaseCatalog():
            Flow<List<DiseaseDomainModel>> {
        return diseaseDao
            .observeDiseases()
            .map { entities ->
                entities.map(
                    diseaseMapper::toDomain
                )
            }
    }

    override fun observeSymptoms():
            Flow<List<SymptomDomainModel>> {
        return symptomDao
            .observeSymptoms()
            .map { entities ->
                entities.map(
                    symptomMapper::toDomain
                )
            }
    }

    override fun observeDiseaseOccurrences(
        blockId: Long
    ): Flow<List<DiseaseOccurrenceDomainModel>> {
        return diseaseOccurrenceDao
            .observeOccurrencesForBlock(blockId)
            .map { entities ->
                entities.map(
                    diseaseOccurrenceMapper::toDomain
                )
            }
    }

    override fun observeActiveAlerts(
        vineyardId: Long
    ): Flow<List<DiseaseAlertDomainModel>> {
        return diseaseAlertDao
            .observeActiveAlerts(vineyardId)
            .map { entities ->
                entities.map(
                    diseaseAlertMapper::toDomain
                )
            }
    }

    override suspend fun getDiseaseOccurrence(
        occurrenceId: Long
    ): DiseaseOccurrenceDomainModel? {
        return diseaseOccurrenceDao
            .getDiseaseOccurrenceById(occurrenceId)
            ?.let(diseaseOccurrenceMapper::toDomain)
    }

    override suspend fun deleteDiseaseOccurrence(
        occurrenceId: Long
    ) {
        diseaseOccurrenceDao
            .deleteDiseaseOccurrenceById(occurrenceId)
    }


    override suspend fun getDisease(
        diseaseId: Long
    ): DiseaseDomainModel? {
        require(diseaseId > 0L) {
            "Disease ID must be positive."
        }

        return diseaseDao
            .getDiseaseById(diseaseId)
            ?.let(diseaseMapper::toDomain)
    }

    override suspend fun createDisease(
        disease: DiseaseDomainModel
    ): Long {
        require(disease.diseaseId == 0L) {
            "New disease must have ID 0."
        }

        val entity = diseaseMapper.toEntity(
            disease.copy(
                isPredefined = false
            )
        )

        return diseaseDao.insertDisease(entity)
    }

    override suspend fun updateDisease(
        disease: DiseaseDomainModel
    ): Boolean {
        require(disease.diseaseId > 0L) {
            "Disease ID must be positive."
        }

        val updatedRows = diseaseDao.updateCustomDisease(
            diseaseId = disease.diseaseId,
            code = disease.code,
            name = disease.name,
            description = disease.description,
            vectors = disease.vectors,
            factors = disease.factors,
            treatmentSuggestion = disease.treatmentSuggestion
        )

        return updatedRows == 1
    }

    override suspend fun deleteDisease(
        diseaseId: Long
    ): Boolean {
        require(diseaseId > 0L) {
            "Disease ID must be positive."
        }

        val deletedRows =
            diseaseDao.deleteUnreferencedCustomDisease(
                diseaseId = diseaseId
            )

        return deletedRows == 1
    }


    override suspend fun isDiseaseReferenced(
        diseaseId: Long
    ): Boolean {
        require(diseaseId > 0L) {
            "Disease ID must be positive."
        }

        return diseaseDao.isDiseaseReferenced(diseaseId)
    }

}