
package com.rncoding.testvineshield.core.data.local.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryEntity

import kotlinx.coroutines.runBlocking
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
class WeatherHistoryDaoIntegrationTest {

    private lateinit var database: VineshieldDatabase

    private val timestamp = 1_700_000_000_000L

    @Before
    fun setUp() {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        database = Room.inMemoryDatabaseBuilder<VineshieldDatabase>(
            context
        )
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createUser(): Long {
        return database.userDao().insertUser(
            UserEntity(
                email = "weather-test@example.com",
                passwordHash = "test-hash",
                passwordSalt = "test-salt"
            )
        )
    }

    private suspend fun createVineyard(
        userId: Long,
        name: String,
        sortOrder: Int = 0
    ): Long {
        return database.vineyardDao().insertVineyard(
            VineyardEntity(
                userId = userId,
                name = name,
                size = 2.5,
                country = "Bulgaria",
                city = "Sofia",
                latitude = 42.7,
                longitude = 23.3,
                timeZone = "Europe/Sofia",
                elevation = 550,
                sortOrder = sortOrder,
                createdAt = LocalDate(2026, 10, 23),
                updatedAt = LocalDate(2026, 10, 23)
            )
        )
    }

    private fun weather(
        vineyardId: Long,
        timestamp: Long = this.timestamp,
        temperature: Double = 21.5
    ): WeatherHistoryEntity {
        return WeatherHistoryEntity(
            vineyardId = vineyardId,
            timestamp = timestamp,
            hourlyTemp2m = temperature,
            relativeHumidity2m = 75.0,
            dewPoint2m = 16.0,
            cloudCover = 40.0,
            windSpeed10m = 8.0,
            precipitation = 1.2,
            rain = 1.0,
            showers = 0.2,
            snowfall = 0.0,
            snowDepth = 0.0,
            weatherCode = 61,
            freezingLevelHeight = 3000.0,
            visibility = 10000.0,
            soilTemperature0cm = 19.0,
            soilTemperature6cm = 18.0,
            soilMoisture0to1cm = 0.25,
            soilMoisture1to3cm = 0.30,
            isDay = true
        )
    }

    @Test
    fun insertedWeatherCanBeRetrieved(): Unit = runBlocking {
        val userId = createUser()
        val vineyardId = createVineyard(userId, "Vineyard A")

        val insertResults = database.weatherHistoryDao()
            .insertMissingWeather(
                listOf(weather(vineyardId))
            )

        assertEquals(1, insertResults.size)
        assertTrue(insertResults.single() > 0L)

        val stored = database.weatherHistoryDao()
            .getLatestWeatherByVineyard(vineyardId)

        assertNotNull(stored)
        assertEquals(vineyardId, stored.vineyardId)
        assertEquals(timestamp, stored.timestamp)
        assertEquals(21.5, stored.hourlyTemp2m)
    }

    @Test
    fun duplicateTimestampIsIgnoredAndOriginalRowPreserved(): Unit =
        runBlocking {
            val userId = createUser()
            val vineyardId = createVineyard(userId, "Vineyard A")

            val firstInsert = database.weatherHistoryDao()
                .insertMissingWeather(
                    listOf(
                        weather(
                            vineyardId = vineyardId,
                            temperature = 21.5
                        )
                    )
                )

            val secondInsert = database.weatherHistoryDao()
                .insertMissingWeather(
                    listOf(
                        weather(
                            vineyardId = vineyardId,
                            temperature = 99.0
                        )
                    )
                )

            assertTrue(firstInsert.single() > 0L)
            assertEquals(-1L, secondInsert.single())

            val stored = database.weatherHistoryDao()
                .getLatestWeatherByVineyard(vineyardId)

            assertNotNull(stored)
            assertEquals(21.5, stored.hourlyTemp2m)

            val allWeather = database.weatherHistoryDao()
                .getWeatherForRiskWindow(
                    vineyardId = vineyardId,
                    startTimestamp = timestamp,
                    endTimestamp = timestamp + 3_600_000L
                )

            assertEquals(1, allWeather.size)
        }

    @Test
    fun sameTimestampIsAllowedForDifferentVineyards(): Unit = runBlocking {
        val userId = createUser()

        val firstVineyardId = createVineyard(
            userId = userId,
            name = "Vineyard A",
            sortOrder = 0
        )

        val secondVineyardId = createVineyard(
            userId = userId,
            name = "Vineyard B",
            sortOrder = 1
        )

        val firstResult = database.weatherHistoryDao()
            .insertMissingWeather(
                listOf(weather(firstVineyardId))
            )

        val secondResult = database.weatherHistoryDao()
            .insertMissingWeather(
                listOf(weather(secondVineyardId))
            )

        assertTrue(firstResult.single() > 0L)
        assertTrue(secondResult.single() > 0L)

        val firstWeather = database.weatherHistoryDao()
            .getLatestWeatherByVineyard(firstVineyardId)

        val secondWeather = database.weatherHistoryDao()
            .getLatestWeatherByVineyard(secondVineyardId)

        assertNotNull(firstWeather)
        assertNotNull(secondWeather)

        assertEquals(timestamp, firstWeather.timestamp)
        assertEquals(timestamp, secondWeather.timestamp)
    }

    @Test
    fun differentTimestampsAreAllowedForSameVineyard(): Unit = runBlocking {
        val userId = createUser()
        val vineyardId = createVineyard(userId, "Vineyard A")

        val secondTimestamp = timestamp + 3_600_000L

        val results = database.weatherHistoryDao()
            .insertMissingWeather(
                listOf(
                    weather(vineyardId, timestamp),
                    weather(vineyardId, secondTimestamp)
                )
            )

        assertEquals(2, results.count { it > 0L })

        val stored = database.weatherHistoryDao()
            .getWeatherForRiskWindow(
                vineyardId = vineyardId,
                startTimestamp = timestamp,
                endTimestamp = secondTimestamp + 1L
            )

        assertEquals(2, stored.size)
        assertEquals(
            listOf(timestamp, secondTimestamp),
            stored.map { it.timestamp }
        )
    }

    @Test
    fun deletingVineyardCascadesToWeatherHistory(): Unit = runBlocking {
        val userId = createUser()
        val vineyardId = createVineyard(userId, "Vineyard A")

        database.weatherHistoryDao().insertMissingWeather(
            listOf(weather(vineyardId))
        )

        val beforeDeletion = database.weatherHistoryDao()
            .getLatestWeatherByVineyard(vineyardId)

        assertNotNull(beforeDeletion)

        val deletedCount = database.vineyardDao()
            .deleteVineyard(
                userId = userId,
                vineyardId = vineyardId
            )

        assertEquals(1, deletedCount)

        val afterDeletion = database.weatherHistoryDao()
            .getLatestWeatherByVineyard(vineyardId)

        assertNull(afterDeletion)
    }
}
