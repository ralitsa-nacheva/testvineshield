
package com.rncoding.testvineshield.core.data.disease_risk

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryEntity
import com.rncoding.testvineshield.core.domain.disease_risk.*
import com.rncoding.testvineshield.di.commonModule
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DiseaseRiskEngineEndToEndTest {

    @Test
    fun evaluatesWeatherPersistsStateAndSkipsAlreadyProcessedDay() =
        runBlocking {

            val context = InstrumentationRegistry
                .getInstrumentation()
                .targetContext

            val database =
                Room.inMemoryDatabaseBuilder<VineshieldDatabase>(context)
                    .setDriver(BundledSQLiteDriver())
                    .build()

            val testDatabaseModule = module {
                single<VineshieldDatabase> {
                    database
                }
            }

            val app = koinApplication {
                modules(commonModule, testDatabaseModule)
            }

            try {
                // 1. Insert the reusable parent records.
                val ids = database.insertDiseaseRiskTestData()

                val modelType =
                    DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS

                val monitoringDate = LocalDate(2026, 10, 19)
                val evaluationDate = LocalDate(2026, 10, 20)
                val zone = TimeZone.of("Europe/Sofia")

                val startTimestamp = monitoringDate
                    .atStartOfDayIn(zone)
                    .toEpochMilliseconds()

                val calculatedAt = evaluationDate
                    .atStartOfDayIn(zone)
                    .toEpochMilliseconds()

                // 2. Enable monitoring for the selected block.
                val monitoringRepository =
                    app.koin.get<
                            DiseaseRiskMonitoringConfigurationRepository
                            >()

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

                // 3. Insert one complete day of hourly weather.
                val weather = (0 until 24).map { hour ->
                    WeatherHistoryEntity(
                        vineyardId = ids.vineyardId,
                        timestamp =
                            startTimestamp + hour * 3_600_000L,
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

                database.weatherHistoryDao()
                    .insertMissingWeather(weather)

                // 4. Resolve the real engine through Koin.
                val engine = app.koin.get<DiseaseRiskEngine>()

                // 5. Execute the real UC Davis model.
                val firstResult = engine.evaluate(
                    modelType = modelType,
                    diseaseId = ids.diseaseId,
                    vineyardId = ids.vineyardId,
                    blockId = ids.blockId,
                    calculatedAt = calculatedAt
                )

                val success =
                    assertIs<DiseaseRiskEngineResult.Success>(
                        firstResult
                    )

                assertEquals(
                    DiseaseRiskEvaluationStatus.CALCULATED,
                    success.evaluation.result.evaluationStatus
                )

                // 6. Verify the evaluation exists in Room.
                val storedEvaluation =
                    database.diseaseRiskDao()
                        .getLatestEvaluation(
                            vineyardId = ids.vineyardId,
                            blockId = ids.blockId,
                            diseaseId = ids.diseaseId,
                            modelType = modelType.name
                        )

                assertNotNull(storedEvaluation)
                assertEquals(
                    calculatedAt,
                    storedEvaluation.calculatedAt
                )

                // 7. Verify model state was persisted.
                val storedState = database.diseaseRiskDao()
                    .getModelState(
                        vineyardId = ids.vineyardId,
                        blockId = ids.blockId,
                        diseaseId = ids.diseaseId,
                        modelType = modelType.name
                    )

                assertNotNull(storedState)

                // 8. Verify the persisted state can be decoded.
                val persistence =
                    app.koin.get<DiseaseRiskPersistence>()

                val model = app.koin
                    .get<DiseaseRiskModelRegistry>()
                    .requireModel(modelType)

                val loaded = persistence.loadState(
                    vineyardId = ids.vineyardId,
                    blockId = ids.blockId,
                    diseaseId = ids.diseaseId,
                    model = model
                )

                assertIs<
                        DiseaseRiskPersistence.StateLoadResult.Loaded
                        >(loaded)

                // 9. Evaluate the same day again.
                val secondResult = engine.evaluate(
                    modelType = modelType,
                    diseaseId = ids.diseaseId,
                    vineyardId = ids.vineyardId,
                    blockId = ids.blockId,
                    calculatedAt = calculatedAt
                )

                val skipped =
                    assertIs<DiseaseRiskEngineResult.Skipped>(
                        secondResult
                    )

                assertEquals(
                    DiseaseRiskEngineResult.Skipped
                        .SkipReason.NO_NEW_WEATHER,
                    skipped.reason
                )

                // The first result remains stored.
                val latestEvaluation =
                    database.diseaseRiskDao()
                        .getLatestEvaluation(
                            vineyardId = ids.vineyardId,
                            blockId = ids.blockId,
                            diseaseId = ids.diseaseId,
                            modelType = modelType.name
                        )

                assertNotNull(latestEvaluation)
                assertEquals(
                    calculatedAt,
                    latestEvaluation.calculatedAt
                )

            } finally {
                app.close()
                database.close()
            }
        }


    @Test
    fun disabledMonitoringSkipsEvaluationWithoutPersisting() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val database =
            Room.inMemoryDatabaseBuilder<VineshieldDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()

        val testDatabaseModule = module {
            single<VineshieldDatabase> { database }
        }

        val app = koinApplication {
            modules(commonModule, testDatabaseModule)
        }

        try {
            val ids = database.insertDiseaseRiskTestData()

            val modelType =
                DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS

            val monitoringRepository = app.koin.get<
                    DiseaseRiskMonitoringConfigurationRepository
                    >()

            monitoringRepository.saveConfiguration(
                DiseaseRiskMonitoringConfiguration(
                    vineyardId = ids.vineyardId,
                    blockId = ids.blockId,
                    diseaseId = ids.diseaseId,
                    modelType = modelType,
                    monitoringStartDate = LocalDate(2026, 10, 19),
                    enabled = false
                )
            )

            val engine = app.koin.get<DiseaseRiskEngine>()

            val calculatedAt = LocalDate(2026, 10, 20)
                .atStartOfDayIn(TimeZone.of("Europe/Sofia"))
                .toEpochMilliseconds()

            val result = engine.evaluate(
                modelType = modelType,
                diseaseId = ids.diseaseId,
                vineyardId = ids.vineyardId,
                blockId = ids.blockId,
                calculatedAt = calculatedAt
            )

            val skipped =
                assertIs<DiseaseRiskEngineResult.Skipped>(result)

            assertEquals(
                DiseaseRiskEngineResult.Skipped
                    .SkipReason.MONITORING_DISABLED,
                skipped.reason
            )

            val storedEvaluation = database.diseaseRiskDao()
                .getLatestEvaluation(
                    vineyardId = ids.vineyardId,
                    blockId = ids.blockId,
                    diseaseId = ids.diseaseId,
                    modelType = modelType.name
                )

            val storedState = database.diseaseRiskDao()
                .getModelState(
                    vineyardId = ids.vineyardId,
                    blockId = ids.blockId,
                    diseaseId = ids.diseaseId,
                    modelType = modelType.name
                )

            assertNull(storedEvaluation)
            assertNull(storedState)

        } finally {
            app.close()
            database.close()
        }
    }

}
