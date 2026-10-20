
package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryRefreshResult
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEngineResult
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelType
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskMonitoringConfiguration
import com.rncoding.testvineshield.core.domain.disease_risk.VineyardDiseaseRiskEvaluationItem
import com.rncoding.testvineshield.core.domain.disease_risk.VineyardDiseaseRiskEvaluationSummary
import com.rncoding.testvineshield.core.domain.disease_risk.VineyardDiseaseRiskEvaluator
import com.rncoding.testvineshield.core.domain.repository.WeatherHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefreshWeatherHistoryAndEvaluateRiskUseCaseTest {

    private val vineyardId = 10L
    private val fetchedAt = 1_792_000_000_000L

    private fun weatherResult(
        insertedHours: Int = 3,
        receivedCompletedHours: Int = 24
    ) = WeatherHistoryRefreshResult(
        vineyardId = vineyardId,
        fetchedAt = fetchedAt,
        receivedCompletedHours = receivedCompletedHours,
        insertedHours = insertedHours
    )

    private fun configuration(
        blockId: Long
    ) = DiseaseRiskMonitoringConfiguration(
        vineyardId = vineyardId,
        blockId = blockId,
        diseaseId = 20L,
        modelType = DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS,
        monitoringStartDate = LocalDate(2026, 10, 1),
        enabled = true
    )

    private fun riskSummary(
        vararg results: DiseaseRiskEngineResult
    ) = VineyardDiseaseRiskEvaluationSummary(
        vineyardId = vineyardId,
        calculatedAt = fetchedAt,
        items = results.mapIndexed { index, result ->
            VineyardDiseaseRiskEvaluationItem(
                configuration = configuration(index.toLong() + 1L),
                result = result
            )
        }
    )

    private class FakeWeatherRepository(
        private val events: MutableList<String>
    ) : WeatherHistoryRepository {

        lateinit var refreshResult: WeatherHistoryRefreshResult

        var refreshException: Exception? = null
        var refreshCalls = 0

        override suspend fun refreshWeatherHistory(
            vineyardId: Long,
            latitude: Double,
            longitude: Double
        ): WeatherHistoryRefreshResult {
            refreshCalls++
            events += "weather"

            refreshException?.let { throw it }

            return refreshResult
        }

        override fun observeLatestVineyardWeather(
            vineyardId: Long
        ): Flow<WeatherHistoryDomainModel?> = emptyFlow()

        override fun observeHistoricalVineyardWeather(
            vineyardId: Long,
            startTimestamp: Long
        ): Flow<List<WeatherHistoryDomainModel>> = emptyFlow()
    }

    private class FakeRiskEvaluator(
        private val events: MutableList<String>
    ) : VineyardDiseaseRiskEvaluator {

        lateinit var evaluationSummary:
                VineyardDiseaseRiskEvaluationSummary

        var evaluationException: Exception? = null
        var evaluationCalls = 0
        var receivedVineyardId: Long? = null
        var receivedCalculatedAt: Long? = null

        override suspend fun evaluateVineyard(
            vineyardId: Long,
            calculatedAt: Long
        ): VineyardDiseaseRiskEvaluationSummary {
            evaluationCalls++
            receivedVineyardId = vineyardId
            receivedCalculatedAt = calculatedAt
            events += "risk"

            evaluationException?.let { throw it }

            return evaluationSummary
        }
    }

    private class Fixture {
        val events = mutableListOf<String>()
        val weather = FakeWeatherRepository(events)
        val risk = FakeRiskEvaluator(events)

        val useCase = RefreshWeatherHistoryAndEvaluateRiskUseCase(
            weatherHistoryRepository = weather,
            vineyardDiseaseRiskEvaluator = risk
        )
    }

    @Test
    fun successfulRefreshEvaluatesRiskAfterWeatherPersistence(): Unit =
        runBlocking {
            val fixture = Fixture()

            fixture.weather.refreshResult = weatherResult()
            fixture.risk.evaluationSummary = riskSummary(
                DiseaseRiskEngineResult.Skipped(
                    DiseaseRiskEngineResult.Skipped
                        .SkipReason.NO_NEW_WEATHER
                )
            )

            val result = fixture.useCase(
                vineyardId = vineyardId,
                latitude = 42.7,
                longitude = 23.3
            )

            assertEquals(
                listOf("weather", "risk"),
                fixture.events
            )

            assertEquals(1, fixture.weather.refreshCalls)
            assertEquals(1, fixture.risk.evaluationCalls)
            assertEquals(vineyardId, fixture.risk.receivedVineyardId)
            assertEquals(fetchedAt, fixture.risk.receivedCalculatedAt)

            assertEquals(vineyardId, result.vineyardId)
            assertEquals(3, result.insertedWeatherHours)
            assertEquals(1, result.evaluatedConfigurations)
            assertEquals(1, result.skippedEvaluations)
            assertFalse(result.hasPartialFailures)
        }

    @Test
    fun zeroInsertedHoursStillTriggersRiskEvaluation(): Unit =
        runBlocking {
            val fixture = Fixture()

            fixture.weather.refreshResult = weatherResult(
                insertedHours = 0
            )
            fixture.risk.evaluationSummary = riskSummary()

            val result = fixture.useCase(
                vineyardId,
                42.7,
                23.3
            )

            assertEquals(0, result.insertedWeatherHours)
            assertEquals(0, result.evaluatedConfigurations)
            assertEquals(1, fixture.risk.evaluationCalls)
            assertEquals(
                listOf("weather", "risk"),
                fixture.events
            )
        }

    @Test
    fun weatherFailurePreventsRiskEvaluation(): Unit =
        runBlocking {
            val fixture = Fixture()

            fixture.weather.refreshException =
                IllegalStateException("Weather persistence failed")

            val exception = assertFailsWith<IllegalStateException> {
                fixture.useCase(vineyardId, 42.7, 23.3)
            }

            assertEquals(
                "Weather persistence failed",
                exception.message
            )
            assertEquals(listOf("weather"), fixture.events)
            assertEquals(0, fixture.risk.evaluationCalls)
        }

    @Test
    fun partialRiskFailuresArePreservedInCombinedResult(): Unit =
        runBlocking {
            val fixture = Fixture()

            fixture.weather.refreshResult = weatherResult(
                insertedHours = 2
            )

            fixture.risk.evaluationSummary = riskSummary(
                DiseaseRiskEngineResult.Skipped(
                    DiseaseRiskEngineResult.Skipped
                        .SkipReason.NO_NEW_WEATHER
                ),
                DiseaseRiskEngineResult.Failure(
                    "Model calculation failed"
                ),
                DiseaseRiskEngineResult.StateInvalid(
                    "Invalid persisted state"
                )
            )

            val result = fixture.useCase(
                vineyardId,
                42.7,
                23.3
            )

            assertEquals(2, result.insertedWeatherHours)
            assertEquals(3, result.evaluatedConfigurations)
            assertEquals(0, result.successfulEvaluations)
            assertEquals(1, result.skippedEvaluations)
            assertEquals(2, result.failedEvaluations)
            assertTrue(result.hasPartialFailures)
        }

    @Test
    fun riskEvaluatorExceptionPropagatesAfterWeatherRefresh(): Unit =
        runBlocking {
            val fixture = Fixture()

            fixture.weather.refreshResult = weatherResult()
            fixture.risk.evaluationException =
                IllegalStateException("Configuration database unavailable")

            val exception = assertFailsWith<IllegalStateException> {
                fixture.useCase(vineyardId, 42.7, 23.3)
            }

            assertEquals(
                "Configuration database unavailable",
                exception.message
            )
            assertEquals(
                listOf("weather", "risk"),
                fixture.events
            )
        }

    @Test
    fun cancellationDuringRiskEvaluationPropagates(): Unit =
        runBlocking {
            val fixture = Fixture()

            fixture.weather.refreshResult = weatherResult()
            fixture.risk.evaluationException =
                CancellationException("Cancelled")

            assertFailsWith<CancellationException> {
                fixture.useCase(vineyardId, 42.7, 23.3)
            }

            assertEquals(
                listOf("weather", "risk"),
                fixture.events
            )
        }

    @Test
    fun cancellationDuringWeatherRefreshPreventsRiskEvaluation(): Unit =
        runBlocking {
            val fixture = Fixture()

            fixture.weather.refreshException =
                CancellationException("Cancelled")

            assertFailsWith<CancellationException> {
                fixture.useCase(vineyardId, 42.7, 23.3)
            }

            assertEquals(listOf("weather"), fixture.events)
            assertEquals(0, fixture.risk.evaluationCalls)
        }

    @Test
    fun invalidArgumentsAreRejectedBeforeCallingDependencies(): Unit =
        runBlocking {
            val fixture = Fixture()

            val invalidCoordinates = listOf(
                Double.NaN to 23.3,
                Double.POSITIVE_INFINITY to 23.3,
                91.0 to 23.3,
                -91.0 to 23.3,
                42.7 to 181.0,
                42.7 to -181.0
            )

            assertFailsWith<IllegalArgumentException> {
                fixture.useCase(0L, 42.7, 23.3)
            }

            invalidCoordinates.forEach { (latitude, longitude) ->
                assertFailsWith<IllegalArgumentException> {
                    fixture.useCase(
                        vineyardId,
                        latitude,
                        longitude
                    )
                }
            }

            assertTrue(fixture.events.isEmpty())
            assertEquals(0, fixture.weather.refreshCalls)
            assertEquals(0, fixture.risk.evaluationCalls)
        }
}
