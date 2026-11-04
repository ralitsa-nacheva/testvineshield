package com.rncoding.testvineshield.core.worker

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.disease_risk.VineyardDiseaseRiskEvaluationSummary
import com.rncoding.testvineshield.core.domain.disease_risk.VineyardDiseaseRiskEvaluator
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result as AppResult
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardWeatherLocationRepository
import com.rncoding.testvineshield.core.domain.repository.WeatherHistoryRepository
import com.rncoding.testvineshield.core.domain.security.SessionManager
import com.rncoding.testvineshield.core.domain.startup.AppInitializer
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.usecases.weather.AuthorizeWeatherHistoryBackgroundWorkUseCase
import com.rncoding.testvineshield.core.domain.usecases.weather.RefreshWeatherHistoryAndEvaluateRiskUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

@RunWith(AndroidJUnit4::class)
class WeatherHistoryRiskWorkerInitializationTest {
    private val events = mutableListOf<String>()

    private fun worker(
        initializer: AppInitializer,
        attempt: Int = 0,
        validInput: Boolean = true
    ): WeatherHistoryRiskWorker {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sessionRepository = object : SessionRepository {
            override suspend fun getSession(): AppResult<SessionDomainModel?, AppError> {
                events += "session"
                return AppResult.Success(SessionDomainModel(1L, 1L, 1L, 100_000L))
            }
            override suspend fun saveSession(session: SessionDomainModel): AppResult<Unit, AppError> =
                error("Worker must not change the session")
            override suspend fun clearSession(): AppResult<Unit, AppError> =
                error("Worker must not clear the test session")
        }
        val clock = object : AppClock {
            override fun nowMillis() = 1_000L
            override fun today(timeZoneId: String) = LocalDate(2026, 10, 28)
        }
        val locations = object : VineyardWeatherLocationRepository {
            override suspend fun getLocation(userId: Long, vineyardId: Long): VineyardWeatherLocation {
                events += "location"
                return VineyardWeatherLocation(vineyardId, 42.1, 24.7)
            }
        }
        val weather = object : WeatherHistoryRepository {
            override suspend fun refreshWeatherHistory(
                vineyardId: Long, latitude: Double, longitude: Double
            ): AppResult<WeatherHistoryRefreshResult, AppError> {
                events += "weather"
                return AppResult.Success(WeatherHistoryRefreshResult(vineyardId, 1_000L, 1, 1))
            }
            override fun observeLatestVineyardWeather(vineyardId: Long): Flow<WeatherHistoryDomainModel?> =
                flowOf(null)
            override fun observeHistoricalVineyardWeather(
                vineyardId: Long, startTimestamp: Long
            ): Flow<List<WeatherHistoryDomainModel>> = flowOf(emptyList())
        }
        val evaluator = object : VineyardDiseaseRiskEvaluator {
            override suspend fun evaluateVineyard(
                vineyardId: Long, calculatedAt: Long
            ): VineyardDiseaseRiskEvaluationSummary {
                events += "risk"
                return VineyardDiseaseRiskEvaluationSummary(vineyardId, calculatedAt, emptyList())
            }
        }
        val authorization = AuthorizeWeatherHistoryBackgroundWorkUseCase(
            SessionManager(sessionRepository, clock), locations
        )
        val refresh = RefreshWeatherHistoryAndEvaluateRiskUseCase(weather, evaluator)
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context, workerClassName: String, workerParameters: WorkerParameters
            ): ListenableWorker? {
                if (workerClassName != WeatherHistoryRiskWorker::class.java.name) return null
                return WeatherHistoryRiskWorker(
                    appContext, workerParameters, initializer, authorization, refresh
                )
            }
        }
        return TestListenableWorkerBuilder<WeatherHistoryRiskWorker>(context)
            .setWorkerFactory(factory)
            .setRunAttemptCount(attempt)
            .setInputData(workDataOf(
                WeatherHistoryRiskWorker.KEY_USER_ID to if (validInput) 1L else -1L,
                WeatherHistoryRiskWorker.KEY_VINEYARD_ID to 10L
            ))
            .build()
    }

    @Test
    fun dependentWorkWaitsUntilInitializationCompletes(): Unit = runBlocking {
        val release = CompletableDeferred<Unit>()
        val worker = worker(AppInitializer {
            events += "initialize"
            release.await()
        })
        val result = async(start = CoroutineStart.UNDISPATCHED) { worker.doWork() }
        assertEquals(listOf("initialize"), events)
        assertFalse(result.isCompleted)
        release.complete(Unit)
        assertEquals(ListenableWorker.Result.success(), result.await())
        assertEquals(listOf("initialize", "session", "session", "location", "weather", "risk"), events)
    }

    @Test
    fun invariantFailureStopsBeforeAuthorization(): Unit = runBlocking {
        val result = worker(AppInitializer { throw IllegalStateException("Reserved code") }).doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
        assertEquals(emptyList(), events)
    }

    @Test
    fun unexpectedInitializationFailureHasBoundedRetries(): Unit = runBlocking {
        val initializer = AppInitializer { throw Exception("Storage unavailable") }
        assertEquals(ListenableWorker.Result.retry(), worker(initializer, attempt = 0).doWork())
        assertEquals(ListenableWorker.Result.failure(), worker(initializer, attempt = 3).doWork())
        assertEquals(emptyList(), events)
    }

    @Test
    fun initializationCancellationPropagates(): Unit = runBlocking {
        assertFailsWith<CancellationException> {
            worker(AppInitializer { throw CancellationException("Stopped") }).doWork()
        }
        assertEquals(emptyList(), events)
    }

    @Test
    fun malformedWorkDoesNotInitializeOrReadSession(): Unit = runBlocking {
        val result = worker(AppInitializer { events += "initialize" }, validInput = false).doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
        assertEquals(emptyList(), events)
    }
}
