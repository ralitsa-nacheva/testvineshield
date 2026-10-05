package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.BlockDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseAlertDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseOccurrenceDao
import com.rncoding.testvineshield.core.data.local.database.daos.VineyardDao
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherCalculationsDao
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherHistoryDao
import com.rncoding.testvineshield.core.data.local.mappers.BlockSummaryMapper
import com.rncoding.testvineshield.core.data.local.mappers.DiseaseAlertSummaryMapper
import com.rncoding.testvineshield.core.data.local.mappers.DiseaseOccurrenceSummaryMapper
import com.rncoding.testvineshield.core.data.local.mappers.VineyardMapper
import com.rncoding.testvineshield.core.data.local.mappers.VineyardSummaryMapper
import com.rncoding.testvineshield.core.data.local.mappers.VineyardWeatherRiskSummaryMapper
import com.rncoding.testvineshield.core.data.local.mappers.VineyardWeatherSummaryMapper
import com.rncoding.testvineshield.core.domain.datamodels.BlockSummary
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseAlertSummary
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseOccurrenceSummary
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.VineyardSummary
import com.rncoding.testvineshield.core.domain.datamodels.VineyardWeatherRiskSummary
import com.rncoding.testvineshield.core.domain.datamodels.VineyardWeatherSummary
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VineyardRepositoryImpl(
    private val vineyardDao: VineyardDao,
    private val vineyardMapper: VineyardMapper,
    private val vineyardSummaryMapper: VineyardSummaryMapper,
    private val blockDao: BlockDao,
    private val weatherHistoryDao: WeatherHistoryDao,
    private val weatherCalculationsDao: WeatherCalculationsDao,
    private val diseaseOccurrenceDao: DiseaseOccurrenceDao,
    private val diseaseAlertDao: DiseaseAlertDao,

    private val blockSummaryMapper: BlockSummaryMapper,
    private val vineyardWeatherSummaryMapper: VineyardWeatherSummaryMapper,
    private val vineyardWeatherRiskSummaryMapper: VineyardWeatherRiskSummaryMapper,
    private val diseaseOccurrenceSummaryMapper: DiseaseOccurrenceSummaryMapper,
    private val diseaseAlertSummaryMapper: DiseaseAlertSummaryMapper
) : VineyardRepository {

    override fun observeVineyards(
        userId: Long
    ): Flow<List<VineyardDomainModel>> {

        return vineyardDao
            .observeVineyardsForUser(userId)
            .map { entities ->
                entities.map(
                    vineyardMapper::entityToDomain
                )
            }
    }

    override fun observeVineyardSummaries(
        userId: Long
    ): Flow<List<VineyardSummary>> {

        return vineyardDao
            .observeVineyardSummaries(userId)
            .map { projections ->
                projections.map(
                    vineyardSummaryMapper::projectionToDomain
                )
            }
    }

    override fun observeVineyard(
        userId: Long,
        vineyardId: Long
    ): Flow<VineyardDomainModel?> {

        return vineyardDao
            .observeVineyard(
                userId = userId,
                vineyardId = vineyardId
            )
            .map { entity ->
                entity?.let(
                    vineyardMapper::entityToDomain
                )
            }
    }

    override suspend fun getVineyard(
        userId: Long,
        vineyardId: Long
    ): Result<VineyardDomainModel, AppError> {

        return try {

            val entity =
                vineyardDao.getVineyard(
                    userId = userId,
                    vineyardId = vineyardId
                )

            Result.Success(
                vineyardMapper.entityToDomain(entity)

            )

        } catch (e: Exception) {

            Result.Error(
                DatabaseError.Unknown(
                    operation = DatabaseError.Operation.READ,
                    cause = e
                )
            )
        }
    }

    override suspend fun createVineyard(
        vineyard: VineyardDomainModel
    ): Result<Long, AppError> {

        return try {

            val vineyardId =
                vineyardDao.insertVineyard(
                    vineyardMapper.domainToEntity(
                        vineyard
                    )
                )

            Result.Success(vineyardId)

        } catch (e: Exception) {

            Result.Error(
                DatabaseError.Unknown(
                    operation = DatabaseError.Operation.INSERT,
                    cause = e
                )
            )
        }
    }

    override suspend fun updateVineyard(
        vineyard: VineyardDomainModel
    ): Result<Unit, AppError> {

        return try {

            val rowsUpdated =
                vineyardDao.updateVineyard(
                    vineyardId = vineyard.vineyardId,
                    userId = vineyard.userId,
                    name = vineyard.name,
                    size = vineyard.size,
                    country = vineyard.country,
                    city = vineyard.city,
                    latitude = vineyard.latitude,
                    longitude = vineyard.longitude,
                    timeZone = vineyard.timeZone,
                    elevation = vineyard.elevation,
                    updatedAt = vineyard.updatedAt
                )

            if (rowsUpdated == 0) {
                Result.Error(
                    DatabaseError.NotFound(
                        entity = "Vineyard"
                    )
                )
            } else {
                Result.Success(Unit)
            }

        } catch (e: Exception) {

            Result.Error(
                DatabaseError.Unknown(
                    operation = DatabaseError.Operation.UPDATE,
                    cause = e
                )
            )
        }
    }

    override suspend fun deleteVineyard(
        userId: Long,
        vineyardId: Long
    ): Result<Unit, AppError> {

        return try {

            val rowsDeleted =
                vineyardDao.deleteVineyard(
                    userId = userId,
                    vineyardId = vineyardId
                )

            if (rowsDeleted == 0) {
                Result.Error(
                    DatabaseError.NotFound(
                        entity = "Vineyard"
                    )
                )
            } else {
                Result.Success(Unit)
            }

        } catch (e: Exception) {

            Result.Error(
                DatabaseError.Unknown(
                    operation = DatabaseError.Operation.DELETE,
                    cause = e
                )
            )
        }
    }

    override suspend fun reorderVineyards(
        userId: Long,
        vineyardIds: List<Long>
    ): Result<Unit, AppError> {

        return try {

            vineyardIds.forEachIndexed { index, vineyardId ->

                vineyardDao.updateSortOrder(
                    userId = userId,
                    vineyardId = vineyardId,
                    sortOrder = index
                )
            }

            Result.Success(Unit)

        } catch (e: Exception) {

            Result.Error(
                DatabaseError.Unknown(
                    operation = DatabaseError.Operation.UPDATE,
                    cause = e
                )
            )
        }
    }

    override suspend fun getNextSortOrder(
        userId: Long
    ): Result<Int, AppError> {

        return try {
            Result.Success(
                vineyardDao.getNextSortOrder(userId)
            )
        } catch (e: Exception) {
            Result.Error(
                DatabaseError.Unknown(
                    operation = DatabaseError.Operation.READ,
                    cause = e
                )
            )
        }
    }

    override fun observeBlockSummaries(
        vineyardId: Long
    ): Flow<List<BlockSummary>> {
        return blockDao
            .observeBlockSummariesForVineyard(vineyardId)
            .map { projections ->
                projections.map(
                    blockSummaryMapper::toDomain
                )
            }
    }

    override fun observeLatestWeather(
        vineyardId: Long
    ): Flow<VineyardWeatherSummary?> {
        return weatherHistoryDao
            .observeLatestVineyardWeather(vineyardId)
            .map { entity ->
                entity?.let(
                    vineyardWeatherSummaryMapper::toDomain
                )
            }
    }

    override fun observeLatestWeatherRisk(
        vineyardId: Long
    ): Flow<VineyardWeatherRiskSummary?> {
        return weatherCalculationsDao
            .observeLatestCalculation(vineyardId)
            .map { entity ->
                entity?.let(
                    vineyardWeatherRiskSummaryMapper::toDomain
                )
            }
    }

    override fun observeActiveDiseaseOccurrences(
        vineyardId: Long
    ): Flow<List<DiseaseOccurrenceSummary>> {
        return diseaseOccurrenceDao
            .observeActiveDiseaseOccurrencesForVineyard(
                vineyardId
            )
            .map { projections ->
                projections.map(
                    diseaseOccurrenceSummaryMapper::toDomain
                )
            }
    }

    override fun observeActiveDiseaseAlerts(
        vineyardId: Long
    ): Flow<List<DiseaseAlertSummary>> {
        return diseaseAlertDao
            .observeActiveAlertsForVineyard(
                vineyardId
            )
            .map { projections ->
                projections.map(
                    diseaseAlertSummaryMapper::toDomain
                )
            }
    }
}