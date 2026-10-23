
package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryEntity

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.yield
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class WeatherHistoryDaoTest {

    private lateinit var database: VineshieldDatabase
    private lateinit var dao: WeatherHistoryDao

    private var userId = 0L
    private var vineyardId = 0L

    private val hourMillis = 3_600_000L

    // Represents an hourly timestamp aligned to a half-hour UTC offset.
    private val firstTimestamp = 1_792_089_000_000L

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        database = Room.inMemoryDatabaseBuilder<VineshieldDatabase>(
            context
        )
            .setDriver(BundledSQLiteDriver())
            .build()

        dao = database.weatherHistoryDao()

        userId = database.userDao().insertUser(
            UserEntity(
                email = "weather-test@example.com",
                passwordHash = "test-hash",
                passwordSalt = "test-salt"
            )
        )

        vineyardId = createVineyard(
            name = "Weather Test Vineyard",
            sortOrder = 0
        )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    private suspend fun createVineyard(
        name: String,
        sortOrder: Int
    ): Long {
        return database.vineyardDao().insertVineyard(
            VineyardEntity(
                userId = userId,
                name = name,
                size = 1.0,
                country = "India",
                city = "New Delhi",
                latitude = 28.6,
                longitude = 77.2,
                timeZone = "Asia/Kolkata",
                elevation = 200,
                sortOrder = sortOrder,
                createdAt = LocalDate(2026, 10, 1),
                updatedAt = LocalDate(2026, 10, 1)
            )
        )
    }

    private fun weatherAt(
        hourOffset: Int,
        temperature: Double,
        vineyardId: Long = this.vineyardId
    ): WeatherHistoryEntity {
        return WeatherHistoryEntity(
            vineyardId = vineyardId,
            timestamp = firstTimestamp + hourOffset * hourMillis,
            hourlyTemp2m = temperature,
            relativeHumidity2m = 75.0,
            dewPoint2m = 10.0,
            cloudCover = 50.0,
            windSpeed10m = 5.0,
            precipitation = 0.0,
            rain = 0.0,
            showers = 0.0,
            snowfall = 0.0,
            snowDepth = 0.0,
            weatherCode = 0,
            freezingLevelHeight = 2000.0,
            visibility = 10000.0,
            soilTemperature0cm = 15.0,
            soilTemperature6cm = 14.0,
            soilMoisture0to1cm = 0.2,
            soilMoisture1to3cm = 0.25,
            isDay = true
        )
    }

    private suspend fun getWeather(
        vineyardId: Long = this.vineyardId,
        startTimestamp: Long = firstTimestamp,
        endTimestamp: Long = firstTimestamp + 24 * hourMillis
    ): List<WeatherHistoryEntity> {
        return dao.getWeatherForRiskWindow(
            vineyardId = vineyardId,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp
        )
    }

    // ------------------------------------------------------------
    // 1. Basic persistence
    // ------------------------------------------------------------

    @Test
    fun insertedWeatherCanBeRetrieved(): Unit = runBlocking {
        val observation = weatherAt(
            hourOffset = 0,
            temperature = 21.5
        )

        val results = dao.insertMissingWeather(
            listOf(observation)
        )

        assertEquals(1, results.size)
        assertTrue(results.single() > 0L)

        val stored = dao.getLatestWeatherByVineyard(vineyardId)

        assertNotNull(stored)
        assertEquals(vineyardId, stored.vineyardId)
        assertEquals(firstTimestamp, stored.timestamp)
        assertEquals(21.5, stored.hourlyTemp2m)
    }

    // ------------------------------------------------------------
    // 2. Duplicate prevention and original-row preservation
    // ------------------------------------------------------------

    @Test
    fun duplicateTimestampIsIgnoredAndOriginalRowPreserved(): Unit =
        runBlocking {
            val original = weatherAt(
                hourOffset = 0,
                temperature = 21.5
            )

            val duplicate = original.copy(
                hourlyTemp2m = 99.0
            )

            val firstResult = dao.insertMissingWeather(
                listOf(original)
            )

            val secondResult = dao.insertMissingWeather(
                listOf(duplicate)
            )

            assertTrue(firstResult.single() > 0L)
            assertEquals(-1L, secondResult.single())

            val stored = getWeather(
                endTimestamp = firstTimestamp + hourMillis
            )

            assertEquals(1, stored.size)
            assertEquals(21.5, stored.single().hourlyTemp2m)
        }

    // ------------------------------------------------------------
    // 3. Composite uniqueness: different vineyards
    // ------------------------------------------------------------

    @Test
    fun sameTimestampIsAllowedForDifferentVineyards(): Unit =
        runBlocking {
            val secondVineyardId = createVineyard(
                name = "Second Vineyard",
                sortOrder = 1
            )

            val firstResult = dao.insertMissingWeather(
                listOf(
                    weatherAt(
                        hourOffset = 0,
                        temperature = 20.0,
                        vineyardId = vineyardId
                    )
                )
            )

            val secondResult = dao.insertMissingWeather(
                listOf(
                    weatherAt(
                        hourOffset = 0,
                        temperature = 25.0,
                        vineyardId = secondVineyardId
                    )
                )
            )

            assertTrue(firstResult.single() > 0L)
            assertTrue(secondResult.single() > 0L)

            val firstWeather = getWeather(
                vineyardId = vineyardId
            )

            val secondWeather = getWeather(
                vineyardId = secondVineyardId
            )

            assertEquals(1, firstWeather.size)
            assertEquals(1, secondWeather.size)

            assertEquals(
                firstTimestamp,
                firstWeather.single().timestamp
            )

            assertEquals(
                firstTimestamp,
                secondWeather.single().timestamp
            )

            assertEquals(
                20.0,
                firstWeather.single().hourlyTemp2m
            )

            assertEquals(
                25.0,
                secondWeather.single().hourlyTemp2m
            )
        }

    // ------------------------------------------------------------
    // 4. Multiple timestamps for one vineyard
    // ------------------------------------------------------------

    @Test
    fun differentTimestampsAreAllowedForSameVineyard(): Unit =
        runBlocking {
            val firstHour = weatherAt(0, 10.0)
            val secondHour = weatherAt(1, 11.0)

            val results = dao.insertMissingWeather(
                listOf(firstHour, secondHour)
            )

            assertEquals(2, results.size)
            assertEquals(2, results.count { it > 0L })

            val stored = getWeather(
                endTimestamp = firstTimestamp + 2 * hourMillis
            )

            assertEquals(2, stored.size)

            assertEquals(
                listOf(
                    firstTimestamp,
                    firstTimestamp + hourMillis
                ),
                stored.map { it.timestamp }
            )
        }

    // ------------------------------------------------------------
    // 5. Missing-hour recovery during overlapping refresh
    // ------------------------------------------------------------

    @Test
    fun insertsMissingHourWithoutDuplicatingOrOverwriting(): Unit =
        runBlocking {
            val hour10 = weatherAt(0, 10.0)
            val hour11 = weatherAt(1, 11.0)
            val hour12 = weatherAt(2, 12.0)
            val hour13 = weatherAt(3, 13.0)

            // Initially, the third hourly observation is missing.
            val initialResults = dao.insertMissingWeather(
                listOf(hour10, hour11, hour13)
            )

            assertEquals(
                3,
                initialResults.count { it != -1L }
            )

            // Simulate an overlapping API refresh.
            // Previously stored measurements must not be overwritten.
            val refreshResults = dao.insertMissingWeather(
                listOf(
                    hour10.copy(hourlyTemp2m = 99.0),
                    hour11.copy(hourlyTemp2m = 99.0),
                    hour12,
                    hour13.copy(hourlyTemp2m = 99.0)
                )
            )

            assertEquals(4, refreshResults.size)
            assertEquals(1, refreshResults.count { it != -1L })
            assertEquals(3, refreshResults.count { it == -1L })

            val stored = getWeather(
                endTimestamp = firstTimestamp + 4 * hourMillis
            )

            assertEquals(4, stored.size)

            assertEquals(
                (0..3).map {
                    firstTimestamp + it * hourMillis
                },
                stored.map { it.timestamp }
            )

            assertEquals(
                listOf(10.0, 11.0, 12.0, 13.0),
                stored.map { it.hourlyTemp2m }
            )

            assertTrue(
                stored.zipWithNext().all { (first, second) ->
                    second.timestamp - first.timestamp == hourMillis
                }
            )
        }

    // ------------------------------------------------------------
    // 6. Actual insertion and ignored-row counts
    // ------------------------------------------------------------

    @Test
    fun insertMissingWeatherReturnsActualInsertionResults(): Unit =
        runBlocking {
            val firstHour = weatherAt(0, 10.0)
            val secondHour = weatherAt(1, 11.0)
            val thirdHour = weatherAt(2, 12.0)

            val firstInsert = dao.insertMissingWeather(
                listOf(firstHour, secondHour)
            )

            assertEquals(2, firstInsert.size)
            assertEquals(2, firstInsert.count { it != -1L })

            val overlappingInsert = dao.insertMissingWeather(
                listOf(
                    firstHour.copy(hourlyTemp2m = 99.0),
                    secondHour.copy(hourlyTemp2m = 99.0),
                    thirdHour
                )
            )

            assertEquals(3, overlappingInsert.size)
            assertEquals(1, overlappingInsert.count { it != -1L })
            assertEquals(2, overlappingInsert.count { it == -1L })

            val stored = getWeather(
                endTimestamp = firstTimestamp + 3 * hourMillis
            )

            assertEquals(3, stored.size)

            assertEquals(
                listOf(10.0, 11.0, 12.0),
                stored.map { it.hourlyTemp2m }
            )
        }

    // ------------------------------------------------------------
    // 7. Half-hour UTC offset timestamp preservation
    // ------------------------------------------------------------

    @Test
    fun preservesHalfHourUtcOffsetTimestamps(): Unit =
        runBlocking {
            val observations = (0..3).map { hour ->
                weatherAt(
                    hourOffset = hour,
                    temperature = 20.0 + hour
                )
            }

            dao.insertMissingWeather(observations)

            val stored = getWeather(
                endTimestamp = firstTimestamp + 4 * hourMillis
            )

            assertEquals(4, stored.size)

            assertEquals(
                observations.map { it.timestamp },
                stored.map { it.timestamp }
            )

            assertTrue(
                stored.zipWithNext().all { (first, second) ->
                    second.timestamp - first.timestamp == hourMillis
                }
            )

            // The timestamp must retain its half-hour UTC alignment.
            assertEquals(
                1_800_000L,
                stored.first().timestamp % hourMillis
            )
        }

    // ------------------------------------------------------------
    // 8. Foreign-key cascade deletion
    // ------------------------------------------------------------

    @Test
    fun deletingVineyardCascadesToWeatherHistory(): Unit =
        runBlocking {
            dao.insertMissingWeather(
                listOf(
                    weatherAt(0, 20.0),
                    weatherAt(1, 21.0)
                )
            )

            val beforeDeletion = getWeather()

            assertEquals(2, beforeDeletion.size)

            val deletedCount = database.vineyardDao()
                .deleteVineyard(
                    userId = userId,
                    vineyardId = vineyardId
                )

            assertEquals(1, deletedCount)

            val afterDeletion = getWeather()

            assertTrue(afterDeletion.isEmpty())

            val latest = dao.getLatestWeatherByVineyard(vineyardId)

            assertNull(latest)
        }


    // ------------------------------------------------------------
    // Checkpoint 6D.2
    // Weather history query correctness
    // ------------------------------------------------------------

    // ------------------------------------------------------------
    // 9. Risk-window boundary semantics
    // ------------------------------------------------------------

    @Test
    fun riskWindowIncludesStartAndExcludesEnd(): Unit = runBlocking {
        dao.insertMissingWeather(
            listOf(
                weatherAt(-1, 9.0),
                weatherAt(0, 10.0),
                weatherAt(1, 11.0),
                weatherAt(2, 12.0)
            )
        )

        val records = dao.getWeatherForRiskWindow(
            vineyardId = vineyardId,
            startTimestamp = firstTimestamp,
            endTimestamp = firstTimestamp + 2 * hourMillis
        )

        assertEquals(2, records.size)

        assertEquals(
            listOf(
                firstTimestamp,
                firstTimestamp + hourMillis
            ),
            records.map { it.timestamp }
        )

        assertEquals(
            listOf(10.0, 11.0),
            records.map { it.hourlyTemp2m }
        )

        // An empty half-open interval must return no records.
        val empty = dao.getWeatherForRiskWindow(
            vineyardId = vineyardId,
            startTimestamp = firstTimestamp,
            endTimestamp = firstTimestamp
        )

        assertTrue(empty.isEmpty())
    }

    // ------------------------------------------------------------
    // 10. Latest observation regardless of insertion order
    // ------------------------------------------------------------

    @Test
    fun latestWeatherUsesTimestampNotInsertionOrder(): Unit = runBlocking {
        dao.insertMissingWeather(
            listOf(
                weatherAt(3, 23.0),
                weatherAt(1, 21.0),
                weatherAt(2, 22.0),
                weatherAt(0, 20.0)
            )
        )

        val latest = dao.getLatestWeatherByVineyard(vineyardId)

        assertNotNull(latest)

        assertEquals(
            firstTimestamp + 3 * hourMillis,
            latest.timestamp
        )

        assertEquals(23.0, latest.hourlyTemp2m)
    }

    // ------------------------------------------------------------
    // 11. Latest timestamp and temperature projections
    // ------------------------------------------------------------

    @Test
    fun latestTimestampAndTemperatureQueriesAreCorrect(): Unit =
        runBlocking {
            val secondVineyardId = createVineyard(
                name = "Second Weather Vineyard",
                sortOrder = 1
            )

            // A vineyard with no weather should return null.
            assertNull(dao.getLastWeatherTimestamp(vineyardId))
            assertNull(dao.getLatestHourlyTempByVineyard(vineyardId))
            assertNull(dao.getLatestWeatherByVineyard(vineyardId))

            dao.insertMissingWeather(
                listOf(
                    weatherAt(2, 22.0),
                    weatherAt(0, 20.0),
                    weatherAt(1, 21.0)
                )
            )

            assertEquals(
                firstTimestamp + 2 * hourMillis,
                dao.getLastWeatherTimestamp(vineyardId)
            )

            assertEquals(
                22.0,
                dao.getLatestHourlyTempByVineyard(vineyardId)
            )

            // Another vineyard must remain unaffected.
            assertNull(
                dao.getLastWeatherTimestamp(secondVineyardId)
            )

            assertNull(
                dao.getLatestHourlyTempByVineyard(secondVineyardId)
            )
        }

    // ------------------------------------------------------------
    // 12. Historical Flow ordering and vineyard isolation
    // ------------------------------------------------------------

    @Test
    fun historicalWeatherFlowReturnsOrderedIsolatedRecords(): Unit =
        runBlocking {
            val secondVineyardId = createVineyard(
                name = "Other Vineyard",
                sortOrder = 1
            )

            dao.insertMissingWeather(
                listOf(
                    weatherAt(3, 23.0),
                    weatherAt(1, 21.0),
                    weatherAt(2, 22.0),
                    weatherAt(0, 20.0),
                    weatherAt(
                        hourOffset = 2,
                        temperature = 99.0,
                        vineyardId = secondVineyardId
                    )
                )
            )

            val records = dao.observeHistoricalVineyardWeather(
                vineyardId = vineyardId,
                startTimestamp = firstTimestamp + hourMillis
            ).first()

            assertEquals(3, records.size)

            assertEquals(
                listOf(
                    firstTimestamp + hourMillis,
                    firstTimestamp + 2 * hourMillis,
                    firstTimestamp + 3 * hourMillis
                ),
                records.map { it.timestamp }
            )

            assertEquals(
                listOf(21.0, 22.0, 23.0),
                records.map { it.hourlyTemp2m }
            )

            assertTrue(
                records.all { it.vineyardId == vineyardId }
            )
        }

    // ------------------------------------------------------------
    // 13. Latest-weather Flow reacts to new observations
    // ------------------------------------------------------------

    @Test
    fun latestWeatherFlowUpdatesWhenNewWeatherIsInserted(): Unit =
        runBlocking {
            // Start with one observation so the first Flow value
            // is deterministic.
            dao.insertMissingWeather(
                listOf(weatherAt(0, 20.0))
            )

            val emissions = mutableListOf<WeatherHistoryEntity?>()

            withTimeout(5_000) {
                val collector = async {
                    dao.observeLatestVineyardWeather(vineyardId)
                        .take(2)
                        .toList(emissions)
                }

                // Allow the Flow collector to subscribe.
                yield()

                // Ensure the initial emission has been observed.
                // The next insertion must cause a new query result.
                while (emissions.isEmpty()) {
                    yield()
                }

                dao.insertMissingWeather(
                    listOf(weatherAt(1, 21.0))
                )

                collector.await()
            }

            assertEquals(2, emissions.size)

            assertNotNull(emissions[0])
            assertNotNull(emissions[1])

            assertEquals(
                firstTimestamp,
                emissions[0]?.timestamp
            )

            assertEquals(
                firstTimestamp + hourMillis,
                emissions[1]?.timestamp
            )

            assertEquals(21.0, emissions[1]?.hourlyTemp2m)
        }

    // ------------------------------------------------------------
    // 14. Empty query results
    // ------------------------------------------------------------

    @Test
    fun emptyVineyardWeatherQueriesReturnExpectedValues(): Unit =
        runBlocking {
            assertTrue(
                getWeather().isEmpty()
            )

            assertNull(
                dao.getLatestWeatherByVineyard(vineyardId)
            )

            assertNull(
                dao.getLatestHourlyTempByVineyard(vineyardId)
            )

            assertNull(
                dao.getLastWeatherTimestamp(vineyardId)
            )

            val historical = dao.observeHistoricalVineyardWeather(
                vineyardId = vineyardId,
                startTimestamp = firstTimestamp
            ).first()

            assertTrue(historical.isEmpty())

            val latest = dao.observeLatestVineyardWeather(
                vineyardId = vineyardId
            ).first()

            assertNull(latest)
        }

}
