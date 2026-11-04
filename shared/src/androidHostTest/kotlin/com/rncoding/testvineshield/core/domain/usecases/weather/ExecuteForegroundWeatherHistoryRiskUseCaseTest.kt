package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.datamodels.VineyardWeatherLocation
import com.rncoding.testvineshield.core.domain.error.NetworkError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.scheduler.ForegroundWeatherOutcome
import com.rncoding.testvineshield.core.domain.startup.AppInitializer
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ExecuteForegroundWeatherHistoryRiskUseCaseTest {
    private val initializer = mockk<AppInitializer>()
    private val authorize = mockk<AuthorizeWeatherHistoryBackgroundWorkUseCase>()
    private val refresh = mockk<RefreshWeatherHistoryAndEvaluateRiskUseCase>()
    private val work = ExecuteForegroundWeatherHistoryRiskUseCase(initializer, authorize, refresh)

    private fun authorized() {
        coEvery { initializer.ensureInitialized() } returns Unit
        coEvery { authorize(1, 10) } returns WeatherHistoryWorkAuthorization.Authorized(
            VineyardWeatherLocation(10, 42.7, 23.3)
        )
    }

    @Test fun initializesThenChecksOwnershipThenUsesCurrentCoordinates() = runTest {
        authorized()
        val result = mockk<RefreshWeatherHistoryAndEvaluateRiskResult>()
        every { result.hasPartialFailures } returns false
        coEvery { refresh(10, 42.7, 23.3) } returns Result.Success(result)
        assertEquals(ForegroundWeatherOutcome.SUCCESS, work(1, 10))
        coVerifyOrder { initializer.ensureInitialized(); authorize(1, 10); refresh(10, 42.7, 23.3) }
    }

    @Test fun unauthorizedNeverFetchesWeather() = runTest {
        authorized()
        coEvery { authorize(1, 10) } returns WeatherHistoryWorkAuthorization.NotAuthorized
        assertEquals(ForegroundWeatherOutcome.REMOVE, work(1, 10))
        coVerify(exactly = 0) { refresh(any(), any(), any()) }
    }

    @Test fun deletedVineyardNeverFetchesWeather() = runTest {
        authorized()
        coEvery { authorize(1, 10) } returns WeatherHistoryWorkAuthorization.VineyardNotFound
        assertEquals(ForegroundWeatherOutcome.REMOVE, work(1, 10))
        coVerify(exactly = 0) { refresh(any(), any(), any()) }
    }

    @Test fun timeoutIsRetriedButPermanentHttpFailureIsNot() = runTest {
        authorized()
        coEvery { refresh(10, 42.7, 23.3) } returns Result.Error(NetworkError.Timeout)
        assertEquals(ForegroundWeatherOutcome.RETRY, work(1, 10))
        coEvery { refresh(10, 42.7, 23.3) } returns Result.Error(NetworkError.HttpError(400, null))
        assertEquals(ForegroundWeatherOutcome.FAILED, work(1, 10))
    }

    @Test fun partialDiseaseFailureIsNotReportedAsCompleteSuccess() = runTest {
        authorized()
        val result = mockk<RefreshWeatherHistoryAndEvaluateRiskResult>()
        every { result.hasPartialFailures } returns true
        coEvery { refresh(10, 42.7, 23.3) } returns Result.Success(result)
        assertEquals(ForegroundWeatherOutcome.PARTIAL_FAILURE, work(1, 10))
    }

    @Test fun startupFailurePreventsAuthorizationAndRefresh() = runTest {
        coEvery { initializer.ensureInitialized() } throws IllegalStateException("startup")
        assertFailsWith<IllegalStateException> { work(1, 10) }
        coVerify(exactly = 0) { authorize(any(), any()) }
        coVerify(exactly = 0) { refresh(any(), any(), any()) }
    }

    @Test fun cancellationPropagates() = runTest {
        authorized()
        coEvery { refresh(10, 42.7, 23.3) } throws CancellationException()
        assertFailsWith<CancellationException> { work(1, 10) }
    }
}
