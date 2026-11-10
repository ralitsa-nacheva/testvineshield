package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.domain.datamodels.CachedForecastHour
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherForecastDao
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherForecastEntity
import com.rncoding.testvineshield.core.data.local.mappers.WeatherForecastMapper
import com.rncoding.testvineshield.core.data.datasource.WeatherRemoteDataSource
import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryDomainModel
import com.rncoding.testvineshield.core.domain.repositories.WeatherForecastRepository
import com.rncoding.testvineshield.core.data.local.database.daos.VineyardDao
import com.rncoding.testvineshield.core.data.local.mappers.DatabaseErrorMapper
import com.rncoding.testvineshield.core.data.remote.WeatherNetworkErrorMapper
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeatherForecastRepositoryImpl(
    private val remoteDataSource: WeatherRemoteDataSource,
    private val vineyardDao: VineyardDao,
    private val forecastDao: WeatherForecastDao,
    private val forecastMapper: WeatherForecastMapper,
    private val refreshCoordinator: WeatherRefreshCoordinator = WeatherRefreshCoordinator()
) : WeatherForecastRepository {
    override fun observeCachedForecast(vineyardId: Long): Flow<List<CachedForecastHour>> =
        forecastDao.observeForecastWeather(vineyardId).map { rows ->
            rows.map { CachedForecastHour(entityToDomain(it), it.forecastCreatedAt) }
        }


    override fun observeForecast(
        vineyardId: Long
    ): Flow<List<WeatherHistoryDomainModel>> {
        return forecastDao
            .observeForecastWeather(vineyardId)
            .map { entities ->
                entities.map(::entityToDomain)
            }
    }

    override fun observeNextForecast(
        vineyardId: Long,
        fromTimestamp: Long
    ): Flow<WeatherHistoryDomainModel?> {
        return forecastDao
            .observeNextForecastWeather(
                vineyardId = vineyardId,
                fromTimestamp = fromTimestamp
            )
            .map { entity ->
                entity?.let(::entityToDomain)
            }
    }

    override suspend fun refreshForecast(
        vineyardId: Long,
        latitude: Double,
        longitude: Double,
        fetchedAt: Long
    ): Result<Unit, AppError> {
        if (vineyardId <= 0 || !latitude.isFinite() || latitude !in -90.0..90.0 ||
            !longitude.isFinite() || longitude !in -180.0..180.0 || fetchedAt <= 0 || fetchedAt > Long.MAX_VALUE - 8L * 24 * ONE_HOUR_MILLIS) {
            return Result.Error(WeatherHistoryError.InvalidInput("Invalid forecast request"))
        }
        return refreshCoordinator.withRefresh(vineyardId) { refreshInternal(vineyardId, latitude, longitude, fetchedAt) }
    }

    private suspend fun refreshInternal(vineyardId: Long, latitude: Double, longitude: Double, fetchedAt: Long): Result<Unit, AppError> {
        currentCoroutineContext().ensureActive()
        val timeZone = try {
            vineyardDao.getVineyardTimeZone(vineyardId)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { return Result.Error(DatabaseErrorMapper.map(e, DatabaseError.Operation.READ)) }
            ?: return Result.Error(DatabaseError.NotFound("Vineyard"))
        currentCoroutineContext().ensureActive()
        val response = try {
            remoteDataSource.fetchWeatherForecast(latitude, longitude, timeZone)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { return Result.Error(WeatherNetworkErrorMapper.map(e)) }
        currentCoroutineContext().ensureActive()
        if (response.timezone != timeZone) return Result.Error(WeatherHistoryError.TimeZoneMismatch(timeZone, response.timezone))
        val entities = try {
            val mapped = forecastMapper.weatherDtoToEntities(response, vineyardId, fetchedAt, fetchedAt)
            require(mapped.size >= MINIMUM_FORECAST_HOURS) { "Insufficient forecast coverage" }
            require(mapped.zipWithNext().all { (a,b) -> b.timestamp - a.timestamp == ONE_HOUR_MILLIS }) { "Discontinuous forecast" }
            require(mapped.first().timestamp >= fetchedAt && mapped.first().timestamp - fetchedAt <= ONE_HOUR_MILLIS) { "Forecast begins outside the next hour" }
            require(mapped.last().timestamp <= fetchedAt + 8L * 24 * ONE_HOUR_MILLIS) { "Forecast horizon exceeds the request contract" }
            mapped
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { return Result.Error(NetworkError.SerializationError(e)) }
        currentCoroutineContext().ensureActive()
        try {
            forecastDao.replaceForecast(vineyardId, entities)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { return Result.Error(DatabaseErrorMapper.map(e, DatabaseError.Operation.INSERT)) }
        currentCoroutineContext().ensureActive()
        return Result.Success(Unit)
    }

    private fun entityToDomain(
        entity: WeatherForecastEntity
    ): WeatherHistoryDomainModel {
        return WeatherHistoryDomainModel(
            vineyardId = entity.vineyardId,
            timestamp = entity.timestamp,
            hourlyTemp2m = entity.hourlyTemp2m,
            relativeHumidity2m = entity.relativeHumidity2m,
            dewPoint2m = entity.dewPoint2m,
            cloudCover = entity.cloudCover,
            windSpeed10m = entity.windSpeed10m,
            precipitation = entity.precipitation,
            rain = entity.rain,
            showers = entity.showers,
            snowfall = entity.snowfall,
            snowDepth = entity.snowDepth,
            weatherCode = entity.weatherCode,
            freezingLevelHeight = entity.freezingLevelHeight,
            visibility = entity.visibility,
            soilTemperature0cm = entity.soilTemperature0cm,
            soilTemperature6cm = entity.soilTemperature6cm,
            soilMoisture0to1cm = entity.soilMoisture0to1cm,
            soilMoisture1to3cm = entity.soilMoisture1to3cm,
            isDay = entity.isDay
        )
    }

    private companion object {
        const val ONE_HOUR_MILLIS = 3_600_000L
        const val MINIMUM_FORECAST_HOURS = 24
    }
}

