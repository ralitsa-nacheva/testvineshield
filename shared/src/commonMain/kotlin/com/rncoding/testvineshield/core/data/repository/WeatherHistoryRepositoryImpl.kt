package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.datasource.WeatherRemoteDataSource
import com.rncoding.testvineshield.core.data.local.database.daos.*
import com.rncoding.testvineshield.core.data.local.mappers.*
import com.rncoding.testvineshield.core.data.remote.WeatherNetworkErrorMapper
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.WeatherHistoryRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Clock

class WeatherHistoryRepositoryImpl(
    private val remoteDataSource: WeatherRemoteDataSource,
    private val vineyardDao: VineyardDao,
    private val weatherHistoryMapper: WeatherHistoryMapper,
    private val cacheDao: WeatherCacheDao,
    private val refreshCoordinator: WeatherRefreshCoordinator=WeatherRefreshCoordinator(),
    private val evaluationCoordinator: com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEvaluationCoordinator=com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEvaluationCoordinator()
) : WeatherHistoryRepository {
    override suspend fun refreshWeatherHistory(vineyardId: Long,latitude: Double,longitude: Double): Result<WeatherHistoryRefreshResult,AppError> {
        if(vineyardId<=0 || !latitude.isFinite() || latitude !in -90.0..90.0 || !longitude.isFinite() || longitude !in -180.0..180.0)
            return Result.Error(WeatherHistoryError.InvalidInput("Invalid weather location"))
        return refreshCoordinator.withRefresh(vineyardId) {
            currentCoroutineContext().ensureActive()
            val zone=try { vineyardDao.getVineyardTimeZone(vineyardId) }
                catch(e: CancellationException) { throw e }
                catch(e: Exception) { return@withRefresh Result.Error(DatabaseErrorMapper.map(e,DatabaseError.Operation.READ)) }
                ?: return@withRefresh Result.Error(DatabaseError.NotFound("Vineyard"))
            currentCoroutineContext().ensureActive()
            val request=try { cacheDao.begin(vineyardId,latitude,longitude,zone,"history",Clock.System.now().toEpochMilliseconds()) }
                catch(e: CancellationException) { throw e }
                catch(e: Exception) { return@withRefresh Result.Error(DatabaseErrorMapper.map(e,DatabaseError.Operation.READ)) }
                ?: return@withRefresh Result.Error(WeatherCacheSourceChanged)
            var finished=false
            try {
                val result=fetchAndCommit(request)
                if(result is Result.Error) recordWeatherFailure(cacheDao,request,result.error)
                finished=true
                result
            } finally { if(!finished) recordWeatherFailure(cacheDao,request,null) }
        }
    }
    private suspend fun fetchAndCommit(request: WeatherCacheRequest): Result<WeatherHistoryRefreshResult,AppError> {
        currentCoroutineContext().ensureActive()
        val response=try { remoteDataSource.fetchWeather(request.latitude,request.longitude,request.timeZone) }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { return Result.Error(WeatherNetworkErrorMapper.map(e)) }
        currentCoroutineContext().ensureActive()
        if(response.timezone!=request.timeZone) return Result.Error(WeatherHistoryError.TimeZoneMismatch(request.timeZone,response.timezone))
        val fetchedAt=Clock.System.now().toEpochMilliseconds()
        val rows=try { weatherHistoryMapper.weatherDtoToEntity(response,request.vineyardId,fetchedAt-3_600_000L) }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { return Result.Error(NetworkError.SerializationError(e)) }
        currentCoroutineContext().ensureActive()
        val inserted=try { evaluationCoordinator.withExclusiveEvaluation { cacheDao.completeHistory(request,rows,fetchedAt) } }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { return Result.Error(DatabaseErrorMapper.map(e,DatabaseError.Operation.INSERT)) }
            ?: return Result.Error(WeatherCacheSourceChanged)
        currentCoroutineContext().ensureActive()
        return Result.Success(WeatherHistoryRefreshResult(request.vineyardId,fetchedAt,rows.size,inserted))
    }
    override fun observeLatestVineyardWeather(vineyardId: Long): Flow<WeatherHistoryDomainModel?> =
        cacheDao.observeLatestHistory(vineyardId).map { row -> row?.let(weatherHistoryMapper::entityToDomain) }
    override fun observeHistoricalVineyardWeather(vineyardId: Long,startTimestamp: Long): Flow<List<WeatherHistoryDomainModel>> =
        cacheDao.observeHistory(vineyardId,startTimestamp).map { rows -> rows.map(weatherHistoryMapper::entityToDomain) }
}
