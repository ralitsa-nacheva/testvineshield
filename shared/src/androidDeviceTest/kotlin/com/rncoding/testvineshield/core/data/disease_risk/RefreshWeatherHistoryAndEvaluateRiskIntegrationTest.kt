
package com.rncoding.testvineshield.core.data.disease_risk

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryEntity
import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryRefreshResult
import com.rncoding.testvineshield.core.domain.disease_risk.*
import com.rncoding.testvineshield.core.domain.repository.WeatherHistoryRepository
import com.rncoding.testvineshield.core.domain.usecases.weather.RefreshWeatherHistoryAndEvaluateRiskUseCase
import com.rncoding.testvineshield.di.commonModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RefreshWeatherHistoryAndEvaluateRiskIntegrationTest {

    private class ControlledWeatherHistoryRepository(
        private val fetchedAt: Long
    ) : WeatherHistoryRepository {

        var refreshCalls = 0

        override suspend fun refreshWeatherHistory(
            vineyardId: Long,
            latitude: Double,
            longitude: Double
        ): WeatherHistoryRefreshResult {
            refreshCalls++

            // Weather was already persisted by the test fixture.
            // Zero new inserts must not suppress risk evaluation.
            return WeatherHistoryRefreshResult(
                vineyardId = vineyardId,
                fetchedAt = fetchedAt,
                receivedCompletedHours = 24,
                insertedHours = 0
            )
        }

        override fun observeLatestVineyardWeather(
            vineyardId: Long
        ): Flow<WeatherHistoryDomainModel?> = emptyFlow()

        override fun observeHistoricalVineyardWeather(
            vineyardId: Long,
            startTimestamp: Long
        ): Flow<List<WeatherHistoryDomainModel>> = emptyFlow()
    }

    @Test
    fun combinedUseCaseEvaluatesPersistedWeatherAndSkipsOnRepeat():
            Unit = runBlocking {

        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val database =
            Room.inMemoryDatabaseBuilder<VineshieldDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()

        val monitoringDate = LocalDate(2026, 10, 19)
        val evaluationDate = LocalDate(2026, 10, 20)
        val zone = TimeZone.of("Europe/Sofia")

        val startTimestamp = monitoringDate
            .atStartOfDayIn(zone)
            .toEpochMilliseconds()

        val calculatedAt = evaluationDate
            .atStartOfDayIn(zone)
            .toEpochMilliseconds()

        val controlledWeather =
            ControlledWeatherHistoryRepository(calculatedAt)

        val testModule = module {
            single<VineshieldDatabase> { database }
            single<WeatherHistoryRepository> { controlledWeather }
        }

        val app = koinApplication {
            modules(commonModule, testModule)
        }

        try {
            val ids = database.insertDiseaseRiskTestData()

            val modelType =
                DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS

            // Enable disease-risk monitoring.
            val monitoringRepository =
                app.koin.get<DiseaseRiskMonitoringConfigurationRepository>()

            monitoringRepository.saveConfiguration(
                DiseaseRiskMonitoringConfiguration(
                    vineyardId = ids.vineyardId,
                    blockId = ids.blockId,
                    diseaseId = ids.diseaseId,
                    modelType = modelType,
                    monitoringStartDate = monitoringDate,
                    enabled = true
                )
            )

            // Insert one day of real Room weather records.
            val weather = (0 until 24).map { hour ->
                WeatherHistoryEntity(
                    vineyardId = ids.vineyardId,
                    timestamp = startTimestamp + hour * 3_600_000L,
                    hourlyTemp2m = 22.0,
                    relativeHumidity2m = 70.0,
                    dewPoint2m = 16.0,
                    cloudCover = 20.0,
                    windSpeed10m = 2.0,
                    precipitation = 0.0,
                    rain = 0.0,
                    showers = 0.0,
                    snowfall = 0.0,
                    snowDepth = 0.0,
                    weatherCode = 0,
                    freezingLevelHeight = 3000.0,
                    visibility = 20000.0,
                    soilTemperature0cm = 18.0,
                    soilTemperature6cm = 17.0,
                    soilMoisture0to1cm = 0.2,
                    soilMoisture1to3cm = 0.25,
                    isDay = hour in 7..18
                )
            }

            val insertResults = database.weatherHistoryDao()
                .insertMissingWeather(weather)

            assertEquals(24, insertResults.count { it != -1L })

            // Resolve the real combined use case through Koin.
            val useCase = app.koin
                .get<RefreshWeatherHistoryAndEvaluateRiskUseCase>()

            // First execution.
            val first = useCase(
                vineyardId = ids.vineyardId,
                latitude = 42.14,
                longitude = 24.75
            )

            assertEquals(1, controlledWeather.refreshCalls)
            assertEquals(0, first.insertedWeatherHours)
            assertEquals(1, first.evaluatedConfigurations)
            assertEquals(1, first.successfulEvaluations)
            assertEquals(0, first.failedEvaluations)

            val success = assertIs<DiseaseRiskEngineResult.Success>(
                first.diseaseRisk.items.single().result
            )

            assertEquals(
                DiseaseRiskEvaluationStatus.CALCULATED,
                success.evaluation.result.evaluationStatus
            )

            // Verify that the real engine persisted the evaluation.
            val storedEvaluation = database.diseaseRiskDao()
                .getLatestEvaluation(
                    vineyardId = ids.vineyardId,
                    blockId = ids.blockId,
                    diseaseId = ids.diseaseId,
                    modelType = modelType.name
                )

            assertNotNull(storedEvaluation)
            assertEquals(calculatedAt, storedEvaluation.calculatedAt)

            // Verify persisted model state.
            val storedState = database.diseaseRiskDao()
                .getModelState(
                    vineyardId = ids.vineyardId,
                    blockId = ids.blockId,
                    diseaseId = ids.diseaseId,
                    modelType = modelType.name
                )

            assertNotNull(storedState)

            // Second execution: no new weather, already evaluated.
            val second = useCase(
                vineyardId = ids.vineyardId,
                latitude = 42.14,
                longitude = 24.75
            )

            assertEquals(2, controlledWeather.refreshCalls)
            assertEquals(0, second.insertedWeatherHours)
            assertEquals(1, second.evaluatedConfigurations)
            assertEquals(0, second.successfulEvaluations)
            assertEquals(1, second.skippedEvaluations)
            assertEquals(0, second.failedEvaluations)

            val skipped = assertIs<DiseaseRiskEngineResult.Skipped>(
                second.diseaseRisk.items.single().result
            )

            assertEquals(
                DiseaseRiskEngineResult.Skipped
                    .SkipReason.NO_NEW_WEATHER,
                skipped.reason
            )

            // The original evaluation must remain persisted.
            val latestEvaluation = database.diseaseRiskDao()
                .getLatestEvaluation(
                    vineyardId = ids.vineyardId,
                    blockId = ids.blockId,
                    diseaseId = ids.diseaseId,
                    modelType = modelType.name
                )

            assertNotNull(latestEvaluation)
            assertEquals(calculatedAt, latestEvaluation.calculatedAt)

        } finally {
            app.close()
            database.close()
        }
    }
}
