package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.WeatherHistoryDao
import com.rncoding.testvineshield.core.data.local.mappers.WeatherHistoryMapper
import com.rncoding.testvineshield.core.data.local.database.daos.VineyardDao
import com.rncoding.testvineshield.core.data.datasource.WeatherRemoteDataSource
import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryDomainModel
import com.rncoding.testvineshield.core.domain.repository.WeatherHistoryRepository
import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryRefreshResult
import com.rncoding.testvineshield.core.data.local.mappers.DatabaseErrorMapper
import com.rncoding.testvineshield.core.data.remote.WeatherNetworkErrorMapper
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.WeatherHistoryError
import kotlinx.coroutines.CancellationException

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class WeatherHistoryRepositoryImpl(
    private val remoteDataSource: WeatherRemoteDataSource,
    private val vineyardDao: VineyardDao,
    private val weatherHistoryDao: WeatherHistoryDao,
    private val weatherHistoryMapper: WeatherHistoryMapper
) : WeatherHistoryRepository {



    override suspend fun refreshWeatherHistory(
        vineyardId: Long,
        latitude: Double,
        longitude: Double
    ): Result<WeatherHistoryRefreshResult, AppError> {

        if (vineyardId <= 0L) {
            return Result.Error(
                WeatherHistoryError.InvalidInput(
                    "Vineyard ID must be positive."
                )
            )
        }

        if (!latitude.isFinite() || latitude !in -90.0..90.0) {
            return Result.Error(
                WeatherHistoryError.InvalidInput(
                    "Latitude must be between -90 and 90."
                )
            )
        }

        if (!longitude.isFinite() || longitude !in -180.0..180.0) {
            return Result.Error(
                WeatherHistoryError.InvalidInput(
                    "Longitude must be between -180 and 180."
                )
            )
        }

        val timeZone = try {
            vineyardDao.getVineyardTimeZone(vineyardId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return Result.Error(
                DatabaseErrorMapper.map(
                    exception,
                    DatabaseError.Operation.READ
                )
            )
        } ?: return Result.Error(
            DatabaseError.NotFound(
                entity = "Vineyard($vineyardId)"
            )
        )

        val response = try {
            remoteDataSource.fetchWeather(
                latitude = latitude,
                longitude = longitude,
                timeZone = timeZone
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return Result.Error(
                WeatherNetworkErrorMapper.map(exception)
            )
        }

        if (response.timezone != timeZone) {
            return Result.Error(
                WeatherHistoryError.TimeZoneMismatch(
                    expected = timeZone,
                    actual = response.timezone
                )
            )
        }

        val fetchedAt = Clock.System.now().toEpochMilliseconds()

        val latestCompletedHourStart =
            fetchedAt - 3_600_000L

        val entities = try {
            weatherHistoryMapper.weatherDtoToEntity(
                weatherDto = response,
                vineyardId = vineyardId,
                latestCompletedHourStart = latestCompletedHourStart
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return Result.Error(
                com.rncoding.testvineshield.core.domain.error
                    .NetworkError.SerializationError(exception)
            )
        }

        val insertedHours = try {
            if (entities.isEmpty()) {
                0
            } else {
                weatherHistoryDao.insertMissingWeather(entities)
                    .count { rowId -> rowId != -1L }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return Result.Error(
                DatabaseErrorMapper.map(
                    exception,
                    DatabaseError.Operation.INSERT
                )
            )
        }

        return Result.Success(
            WeatherHistoryRefreshResult(
                vineyardId = vineyardId,
                fetchedAt = fetchedAt,
                receivedCompletedHours = entities.size,
                insertedHours = insertedHours
            )
        )
    }



    override fun observeLatestVineyardWeather(
        vineyardId: Long
    ): Flow<WeatherHistoryDomainModel?> {
        return weatherHistoryDao
            .observeLatestVineyardWeather(vineyardId)
            .map { entity ->
                entity?.let(
                    weatherHistoryMapper::entityToDomain
                )
            }
    }

    override fun observeHistoricalVineyardWeather(
        vineyardId: Long,
        startTimestamp: Long
    ): Flow<List<WeatherHistoryDomainModel>> {
        return weatherHistoryDao
            .observeHistoricalVineyardWeather(
                vineyardId = vineyardId,
                startTimestamp = startTimestamp
            )
            .map { entities ->
                entities.map(
                    weatherHistoryMapper::entityToDomain
                )
            }
    }
}