package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.datasource.WeatherRemoteDataSource
import com.rncoding.testvineshield.core.data.local.database.daos.*
import com.rncoding.testvineshield.core.data.local.mappers.WeatherForecastMapper
import com.rncoding.testvineshield.core.data.remote.WeatherResponseFixtures
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.io.IOException
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherForecastRepositoryImplTest {
    private val remote=mockk<WeatherRemoteDataSource>()
    private val vineyard=mockk<VineyardDao>()
    private val cache=mockk<WeatherCacheDao>(relaxed=true)
    private val mapper=WeatherForecastMapper()
    private val repository=WeatherForecastRepositoryImpl(remote,vineyard,mapper,cache)
    private val fetchedAt=1_800_000_000_000L
    private fun setup() {
        coEvery { cache.begin(10,42.7,23.3,"Europe/Sofia","forecast",any()) } returns WeatherCacheRequest(10,1,"generation","token","forecast",42.7,23.3,"Europe/Sofia",fetchedAt)
        coEvery { vineyard.getVineyardTimeZone(10) } returns "Europe/Sofia"
        coEvery { remote.fetchWeatherForecast(42.7,23.3,"Europe/Sofia") } returns WeatherResponseFixtures.response()
        coEvery { cache.completeForecast(any(),any(),any()) } returns true
    }
    @Test fun invalidInputsNeverTouchStorageOrNetwork() = runTest {
        for ((id,lat,lon,time) in listOf(Request(0,42.7,23.3,fetchedAt),Request(10,Double.NaN,23.3,fetchedAt),Request(10,42.7,181.0,fetchedAt),Request(10,42.7,23.3,0),Request(10,42.7,23.3,Long.MAX_VALUE))) {
            assertIs<WeatherHistoryError.InvalidInput>(assertIs<Result.Error<*>>(repository.refreshForecast(id,lat,lon,time)).error)
        }
        verify { vineyard wasNot Called; remote wasNot Called; cache wasNot Called }
    }
    @Test fun absentParentAndReadFailureAreTyped() = runTest {
        setup(); coEvery { vineyard.getVineyardTimeZone(10) } returns null
        assertIs<DatabaseError.NotFound>(assertIs<Result.Error<*>>(repository.refreshForecast(10,42.7,23.3,fetchedAt)).error)
        coEvery { vineyard.getVineyardTimeZone(10) } throws IllegalStateException("storage")
        val error=assertIs<DatabaseError.Unknown>(assertIs<Result.Error<*>>(repository.refreshForecast(10,42.7,23.3,fetchedAt)).error)
        assertEquals(DatabaseError.Operation.READ,error.operation); verify { remote wasNot Called }; coVerify(exactly=0) { cache.completeForecast(any(),any(),any()) }
    }
    @Test fun networkFailureDoesNotReplaceCache() = runTest {
        setup(); coEvery { remote.fetchWeatherForecast(42.7,23.3,"Europe/Sofia") } throws IOException("unreachable")
        assertEquals(NetworkError.NoInternet,assertIs<Result.Error<*>>(repository.refreshForecast(10,42.7,23.3,fetchedAt)).error)
        coVerify(exactly=0) { cache.completeForecast(any(),any(),any()) }
    }
    @Test fun zoneMismatchDoesNotReplaceCache() = runTest {
        setup(); coEvery { remote.fetchWeatherForecast(42.7,23.3,"Europe/Sofia") } returns WeatherResponseFixtures.response().copy(timezone="UTC")
        assertIs<WeatherHistoryError.TimeZoneMismatch>(assertIs<Result.Error<*>>(repository.refreshForecast(10,42.7,23.3,fetchedAt)).error)
        coVerify(exactly=0) { cache.completeForecast(any(),any(),any()) }
    }
    @Test fun wrongUnitsAndNonfiniteDataNeverReachReplacement() = runTest {
        setup(); val good=WeatherResponseFixtures.response()
        for (response in listOf(good.copy(hourlyUnits=good.hourlyUnits!! + ("temperature_2m" to "°F")),
            good.copy(hourlyWeather=good.hourlyWeather.copy(hourlyTemp2m=List(48) { Double.NaN })))) {
            coEvery { remote.fetchWeatherForecast(42.7,23.3,"Europe/Sofia") } returns response
            assertIs<NetworkError.SerializationError>(assertIs<Result.Error<*>>(repository.refreshForecast(10,42.7,23.3,fetchedAt)).error)
        }
        coVerify(exactly=0) { cache.completeForecast(any(),any(),any()) }
    }
    @Test fun shortDistantAndExcessiveHorizonsNeverReplaceCache() = runTest {
        setup()
        for (response in listOf(WeatherResponseFixtures.response(23),WeatherResponseFixtures.response(startSeconds=fetchedAt/1000+7200),WeatherResponseFixtures.response(768))) {
            coEvery { remote.fetchWeatherForecast(42.7,23.3,"Europe/Sofia") } returns response
            assertIs<NetworkError.SerializationError>(assertIs<Result.Error<*>>(repository.refreshForecast(10,42.7,23.3,fetchedAt)).error)
        }
        coVerify(exactly=0) { cache.completeForecast(any(),any(),any()) }
    }
    @Test fun validBatchKeepsNumbersFlagsAndRequestStartThenReplacesOnce() = runTest {
        setup(); assertIs<Result.Success<Unit>>(repository.refreshForecast(10,42.7,23.3,fetchedAt))
        coVerify(exactly=1) { cache.completeForecast(any(),match { rows ->
            rows.size==48 && rows.first().timestamp==fetchedAt && rows.all { it.forecastCreatedAt==fetchedAt && it.vineyardId==10L } && !rows[0].isDay && rows[1].isDay
        },any()) }
    }
    @Test fun cacheWriteFailureIsTypedAndRetryStillWorks() = runTest {
        setup(); coEvery { cache.completeForecast(any(),any(),any()) } throws IllegalStateException("storage")
        val error=assertIs<DatabaseError.Unknown>(assertIs<Result.Error<*>>(repository.refreshForecast(10,42.7,23.3,fetchedAt)).error)
        assertEquals(DatabaseError.Operation.INSERT,error.operation)
        coEvery { cache.completeForecast(any(),any(),any()) } returns true
        assertIs<Result.Success<Unit>>(repository.refreshForecast(10,42.7,23.3,fetchedAt))
    }
    @Test fun cancellationAfterLocationLookupPreventsNetwork() = runTest {
        setup(); coEvery { vineyard.getVineyardTimeZone(10) } coAnswers {
            currentCoroutineContext().job.cancel(); "Europe/Sofia"
        }
        val request=async { repository.refreshForecast(10,42.7,23.3,fetchedAt) }
        assertFailsWith<CancellationException> { request.await() }
        verify { remote wasNot Called }; coVerify(exactly=0) { cache.completeForecast(any(),any(),any()) }
    }
    @Test fun cancellationDuringFetchCleansUpWithoutWriting() = runTest {
        setup(); val entered=CompletableDeferred<Unit>(); var cleaned=false
        coEvery { remote.fetchWeatherForecast(42.7,23.3,"Europe/Sofia") } coAnswers {
            try { entered.complete(Unit); awaitCancellation() } finally { cleaned=true }
        }
        val request=launch { repository.refreshForecast(10,42.7,23.3,fetchedAt) }
        entered.await(); request.cancelAndJoin(); assertTrue(cleaned); coVerify(exactly=0) { cache.completeForecast(any(),any(),any()) }
        coEvery { remote.fetchWeatherForecast(42.7,23.3,"Europe/Sofia") } returns WeatherResponseFixtures.response()
        assertIs<Result.Success<Unit>>(repository.refreshForecast(10,42.7,23.3,fetchedAt))
    }
    @Test fun cancellationDuringReplacementPropagates() = runTest {
        setup(); coEvery { cache.completeForecast(any(),any(),any()) } throws CancellationException("cancel")
        assertFailsWith<CancellationException> { repository.refreshForecast(10,42.7,23.3,fetchedAt) }
    }
    @Test fun historyAndForecastShareOneCoordinatorWhenInjected() = runTest {
        setup(); val coordinator=WeatherRefreshCoordinator(); val entered=CompletableDeferred<Unit>(); val release=CompletableDeferred<Unit>()
        val historyRemote=mockk<WeatherRemoteDataSource>(); val historyMapper=mockk<com.rncoding.testvineshield.core.data.local.mappers.WeatherHistoryMapper>()
        val response=WeatherResponseFixtures.response()
        coEvery { cache.begin(10,42.7,23.3,"Europe/Sofia","history",any()) } returns WeatherCacheRequest(10,1,"generation","history-token","history",42.7,23.3,"Europe/Sofia",fetchedAt)
        coEvery { cache.completeHistory(any(),any(),any()) } returns 0
        coEvery { historyRemote.fetchWeather(42.7,23.3,"Europe/Sofia") } coAnswers { entered.complete(Unit); release.await(); response }
        every { historyMapper.weatherDtoToEntity(response,10,any()) } returns emptyList()
        val history=WeatherHistoryRepositoryImpl(historyRemote,vineyard,historyMapper,cache,coordinator)
        val forecast=WeatherForecastRepositoryImpl(remote,vineyard,mapper,cache,coordinator)
        val first=async { history.refreshWeatherHistory(10,42.7,23.3) }; entered.await()
        val second=async { forecast.refreshForecast(10,42.7,23.3,fetchedAt) }; runCurrent()
        coVerify(exactly=0) { remote.fetchWeatherForecast(any(),any(),any()) }
        release.complete(Unit); assertIs<Result.Success<*>>(first.await()); assertIs<Result.Success<*>>(second.await())
    }
    private data class Request(val id: Long,val latitude: Double,val longitude: Double,val time: Long)
}
