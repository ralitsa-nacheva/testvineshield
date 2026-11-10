package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repositories.WeatherForecastRepository
import com.rncoding.testvineshield.core.domain.repository.*
import com.rncoding.testvineshield.core.domain.time.AppClock
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.*

class RefreshOwnedWeatherUseCaseTest {
    private val users = mockk<AuthenticatedUserProvider>()
    private val vineyards = mockk<VineyardRepository>()
    private val history = mockk<WeatherHistoryRepository>()
    private val forecast = mockk<WeatherForecastRepository>()
    private val clock = mockk<AppClock>()
    private val refresh = RefreshOwnedWeatherUseCase(users, vineyards, history, forecast, clock)
    private val parent = VineyardDomainModel(10, 1, "North", 1.0, "BG", "Plovdiv", 42.1, 24.7, "Europe/Sofia", null, 0, LocalDate(2026,1,1), LocalDate(2026,1,1))
    private fun setup() {
        every { users.currentUserOrNull() } returns UserDomainModel(1,"a@example.com")
        every { clock.nowMillis() } returns 2_000_000_000_000L
        coEvery { vineyards.getVineyard(1,10) } returns Result.Success(parent)
        coEvery { history.refreshWeatherHistory(10,42.1,24.7) } returns Result.Success(WeatherHistoryRefreshResult(10, 2_000_000_000_000L, 24, 0))
        coEvery { forecast.refreshForecast(10,42.1,24.7,any()) } returns Result.Success(Unit)
    }
    @Test fun invalidScopeAndMissingOwnerNeverRefresh() = runTest {
        assertIs<Result.Error<*>>(refresh(0))
        every { users.currentUserOrNull() } returns null
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(refresh(10)).error)
        verify { history wasNot Called; forecast wasNot Called }
    }
    @Test fun ownershipAndLocationAreValidatedBeforeNetwork() = runTest {
        setup(); coEvery { vineyards.getVineyard(1,10) } returns Result.Error(DatabaseError.NotFound("Vineyard"))
        assertIs<Result.Error<*>>(refresh(10))
        coEvery { vineyards.getVineyard(1,10) } returns Result.Success(parent.copy(latitude = Double.NaN))
        assertIs<WeatherHistoryError.InvalidInput>(assertIs<Result.Error<*>>(refresh(10)).error)
        coEvery { vineyards.getVineyard(1,10) } returns Result.Success(parent.copy(timeZone = "Invalid/Zone"))
        assertIs<Result.Error<*>>(refresh(10)); verify { history wasNot Called; forecast wasNot Called }
    }
    @Test fun partialFailureIsSeparateAndStillAttemptsOtherCache() = runTest {
        setup(); coEvery { history.refreshWeatherHistory(10,42.1,24.7) } returns Result.Error(NetworkError.Timeout)
        val r = assertIs<Result.Success<WeatherRefreshReport>>(refresh(10)).data
        assertEquals(NetworkError.Timeout, r.historyError); assertNull(r.forecastError)
        coVerify(exactly = 1) { forecast.refreshForecast(10,42.1,24.7,any()) }
    }
    @Test fun forecastExceptionDoesNotDisguiseSuccessfulHistoryOrEscapeAsSuccess() = runTest {
        setup(); coEvery { forecast.refreshForecast(10,42.1,24.7,any()) } throws IllegalStateException("network")
        val r = assertIs<Result.Success<WeatherRefreshReport>>(refresh(10)).data
        assertNull(r.historyError); assertIs<UnexpectedError>(r.forecastError)
    }
    @Test fun logoutOrLocationEditDuringHistoryPreventsSecondRequest() = runTest {
        setup(); coEvery { history.refreshWeatherHistory(10,42.1,24.7) } coAnswers {
            every { users.currentUserOrNull() } returns null
            Result.Success(WeatherHistoryRefreshResult(10, 2_000_000_000_000L, 24, 0))
        }
        assertEquals(AuthError.NoActiveSession, assertIs<Result.Error<*>>(refresh(10)).error)
        coVerify(exactly = 0) { forecast.refreshForecast(any(),any(),any(),any()) }
        setup(); coEvery { vineyards.getVineyard(1,10) } returnsMany listOf(Result.Success(parent), Result.Success(parent.copy(longitude = 25.0)))
        assertIs<WeatherHistoryError.InvalidInput>(assertIs<Result.Error<*>>(refresh(10)).error)
        coVerify(exactly = 0) { forecast.refreshForecast(any(),any(),any(),any()) }
    }
    @Test fun cancellationIsNeverConvertedToReportAndStopsFollowingRequest() = runTest {
        setup(); var cleaned = false; val started = CompletableDeferred<Unit>()
        coEvery { history.refreshWeatherHistory(10,42.1,24.7) } coAnswers {
            try { started.complete(Unit); awaitCancellation() } finally { cleaned = true }
        }
        var returned = false
        val job = launch { refresh(10); returned = true }
        started.await(); job.cancelAndJoin()
        assertTrue(cleaned); assertFalse(returned)
        coVerify(exactly = 0) { forecast.refreshForecast(any(),any(),any(),any()) }
    }
}
