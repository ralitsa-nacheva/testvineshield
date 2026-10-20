
package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
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
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class WeatherHistoryDaoTest {

    private lateinit var database: VineshieldDatabase
    private lateinit var dao: WeatherHistoryDao

    private var vineyardId = 0L

    private val hourMillis = 3_600_000L

    // Half-hour UTC offset, as with Asia/Kolkata.
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

        val userId = database.userDao().insertUser(
            UserEntity(
                email = "weather-test@example.com",
                passwordHash = "test-hash",
                passwordSalt = "test-salt"
            )
        )

        vineyardId = database.vineyardDao().insertVineyard(
            VineyardEntity(
                userId = userId,
                name = "Weather Test Vineyard",
                size = 1.0,
                country = "India",
                city = "New Delhi",
                latitude = 28.6,
                longitude = 77.2,
                timeZone = "Asia/Kolkata",
                elevation = 200,
                sortOrder = 0,
                createdAt = LocalDate(2026, 10, 1),
                updatedAt = LocalDate(2026, 10, 1)
            )
        )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun insertsMissingHourWithoutDuplicatingOrOverwriting() = runBlocking {
        val hour10 = weatherAt(0, 10.0)
        val hour11 = weatherAt(1, 11.0)
        val hour12 = weatherAt(2, 12.0)
        val hour13 = weatherAt(3, 13.0)

        // Simulate an existing gap.
        dao.insertMissingWeather(
            listOf(hour10, hour11, hour13)
        )

        // Simulate an overlapping API refresh.
        // Existing timestamps deliberately have different values.
        dao.insertMissingWeather(
            listOf(
                hour10.copy(hourlyTemp2m = 99.0),
                hour11.copy(hourlyTemp2m = 99.0),
                hour12,
                hour13.copy(hourlyTemp2m = 99.0)
            )
        )

        val records = dao.getWeatherForRiskWindow(
            vineyardId = vineyardId,
            startTimestamp = firstTimestamp,
            endTimestamp = firstTimestamp + 4 * hourMillis
        )

        assertEquals(4, records.size)

        assertEquals(
            (0..3).map {
                firstTimestamp + it * hourMillis
            },
            records.map { it.timestamp }
        )

        // Existing measurements must remain unchanged.
        assertEquals(
            listOf(10.0, 11.0, 12.0, 13.0),
            records.map { it.hourlyTemp2m }
        )

        // Verify strictly continuous hourly timestamps.
        assertEquals(
            true,
            records.zipWithNext().all { (a, b) ->
                b.timestamp - a.timestamp == hourMillis
            }
        )

        // Verify that the half-hour UTC offset is preserved.
        assertEquals(
            1_800_000L,
            records.first().timestamp % hourMillis
        )
    }

    @Test
    fun ignoresDuplicateTimestampOnRepeatedRefresh() = runBlocking {
        val observation = weatherAt(0, 20.0)

        dao.insertMissingWeather(listOf(observation))
        dao.insertMissingWeather(listOf(observation))
        dao.insertMissingWeather(listOf(observation))

        val records = dao.getWeatherForRiskWindow(
            vineyardId = vineyardId,
            startTimestamp = firstTimestamp,
            endTimestamp = firstTimestamp + hourMillis
        )

        assertEquals(1, records.size)
        assertEquals(20.0, records.single().hourlyTemp2m)
    }

    private fun weatherAt(
        hourOffset: Int,
        temperature: Double
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
}
