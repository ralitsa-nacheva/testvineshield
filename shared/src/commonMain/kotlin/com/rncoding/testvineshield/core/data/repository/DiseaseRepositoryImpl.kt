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
import kotlinx.coroutines.flow.Flow
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
}