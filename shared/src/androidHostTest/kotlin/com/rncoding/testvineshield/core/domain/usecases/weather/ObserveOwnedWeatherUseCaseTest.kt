package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.disease_risk.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repositories.WeatherForecastRepository
import com.rncoding.testvineshield.core.domain.repository.*
import com.rncoding.testvineshield.core.domain.time.AppClock
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveOwnedWeatherUseCaseTest {
    private val users = mockk<AuthenticatedUserProvider>()
    private val vineyards = mockk<VineyardRepository>()
    private val history = mockk<WeatherHistoryRepository>()
    private val forecast = mockk<WeatherForecastRepository>()
    private val configs = mockk<DiseaseRiskMonitoringConfigurationRepository>()
    private val blocks = mockk<BlockRepository>()
    private val clock = mockk<AppClock>()
    private val parent = mockk<VineyardDomainModel>()
    private val cacheStatus=mockk<WeatherCacheStatusRepository>()
    private val observe = ObserveOwnedWeatherUseCase(users, vineyards, history, forecast, configs, blocks, clock,cacheStatus)
    private fun setup() {
        every { cacheStatus.observe(10) } returns flowOf(emptyList())
        every { users.observeUser() } returns flowOf(UserDomainModel(1, "a@example.com"))
        every { vineyards.observeVineyard(1, 10) } returns flowOf(parent)
        every { clock.nowMillis() } returns 2_000_000_000_000L
        every { history.observeHistoricalVineyardWeather(10, any()) } returns flowOf(emptyList())
        every { forecast.observeCachedForecast(10) } returns flowOf(emptyList())
        every { configs.observeConfigurationsForVineyard(10) } returns flowOf(emptyList())
        every { blocks.observeBlocks(10) } returns flowOf(emptyList())
    }
    @Test fun invalidIdAndAbsentSessionNeverSubscribeToCaches() = runTest {
        assertIs<Result.Error<*>>(observe(0).first())
        every { users.observeUser() } returns flowOf(null)
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(observe(10).first()).error)
        verify { history wasNot Called; forecast wasNot Called; configs wasNot Called; blocks wasNot Called }
    }
    @Test fun unownedVineyardNeverSubscribesToCaches() = runTest {
        setup(); every { vineyards.observeVineyard(1, 10) } returns flowOf(null)
        assertIs<DatabaseError.NotFound>(assertIs<Result.Error<*>>(observe(10).first()).error)
        verify { history wasNot Called; forecast wasNot Called }
    }
    @Test fun emptyCachesAreARealSuccessWithNoInventedWeather() = runTest {
        setup(); val s = assertIs<Result.Success<VineyardWeatherSnapshot>>(observe(10).first()).data
        assertTrue(s.history.isEmpty()); assertTrue(s.forecast.isEmpty()); assertTrue(s.configurations.isEmpty())
    }
    @Test fun logoutClearsSuccessAndCancelsChildStreams() = runTest {
        setup(); val session = MutableStateFlow<UserDomainModel?>(UserDomainModel(1,"a@example.com"))
        every { users.observeUser() } returns session
        var stopped = false
        every { history.observeHistoricalVineyardWeather(10, any()) } returns flow<List<WeatherHistoryDomainModel>> {
            try { emit(emptyList()); awaitCancellation() } finally { stopped = true }
        }
        val results = mutableListOf<Result<VineyardWeatherSnapshot, AppError>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { observe(10).collect { results.add(it) } }
        runCurrent(); assertIs<Result.Success<*>>(results.last()); session.value = null; runCurrent()
        assertTrue(stopped); assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(results.last()).error)
    }
    @Test fun parentRemovalClearsSuccessAndForeignRowsFailClosed() = runTest {
        setup(); val owned = MutableStateFlow<VineyardDomainModel?>(parent)
        every { vineyards.observeVineyard(1,10) } returns owned
        val results = mutableListOf<Result<VineyardWeatherSnapshot, AppError>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { observe(10).collect { results.add(it) } }
        runCurrent(); owned.value = null; runCurrent(); assertIs<DatabaseError.NotFound>(assertIs<Result.Error<*>>(results.last()).error)
        setup(); val foreign = mockk<WeatherHistoryDomainModel>(); every { foreign.vineyardId } returns 99
        every { history.observeHistoricalVineyardWeather(10, any()) } returns flowOf(listOf(foreign))
        assertIs<UnexpectedError>(assertIs<Result.Error<*>>(observe(10).first()).error)
    }
    @Test fun readFailureIsTypedAndCollectorCancellationCleansUp() = runTest {
        setup(); every { forecast.observeCachedForecast(10) } returns flow { throw IllegalStateException("storage") }
        assertIs<UnexpectedError>(assertIs<Result.Error<*>>(observe(10).first()).error)
        setup(); val started = CompletableDeferred<Unit>(); var stopped = false
        every { history.observeHistoricalVineyardWeather(10, any()) } returns flow<List<WeatherHistoryDomainModel>> {
            try { started.complete(Unit); awaitCancellation() } finally { stopped = true }
        }
        val emissions = mutableListOf<Result<VineyardWeatherSnapshot, AppError>>()
        val job = launch { observe(10).collect { emissions.add(it) } }
        started.await(); job.cancelAndJoin(); assertTrue(stopped); assertTrue(emissions.isEmpty())
    }
    @Test fun monitoringReadFailureDoesNotHideWeatherOrPretendThereAreNoSettings() = runTest {
        setup(); every { configs.observeConfigurationsForVineyard(10) } returns flow { throw IllegalStateException("settings storage") }
        val result = assertIs<Result.Success<VineyardWeatherSnapshot>>(observe(10).first()).data
        assertEquals(parent, result.vineyard); assertIs<UnexpectedError>(result.monitoringError)
    }

}
