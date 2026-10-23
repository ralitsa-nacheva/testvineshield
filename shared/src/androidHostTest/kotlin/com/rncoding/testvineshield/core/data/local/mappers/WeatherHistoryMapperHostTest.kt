
package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.remote.dto.HourlyWeatherDto
import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeatherHistoryMapperHostTest {

    private val mapper = WeatherHistoryMapper()

    private val vineyardId = 10L

    private fun hourlyWeather(
        timestamps: List<Long>
    ): HourlyWeatherDto {
        val count = timestamps.size

        return HourlyWeatherDto(
            timestamp = timestamps,
            hourlyTemp2m = List(count) { 21.5 },
            relativeHumidity2m = List(count) { 75.0 },
            dewPoint2m = List(count) { 16.0 },
            cloudCover = List(count) { 40.0 },
            windSpeed10m = List(count) { 8.0 },
            precipitation = List(count) { 1.2 },
            rain = List(count) { 1.0 },
            showers = List(count) { 0.2 },
            snowfall = List(count) { 0.0 },
            snowDepth = List(count) { 0.0 },
            weatherCode = List(count) { 61 },
            freezingLevelHeight = List(count) { 3000.0 },
            visibility = List(count) { 10000.0 },
            soilTemperature0cm = List(count) { 19.0 },
            soilTemperature6cm = List(count) { 18.0 },
            soilMoisture0to1cm = List(count) { 0.25 },
            soilMoisture1to3cm = List(count) { 0.30 },
            isDay = List(count) { true }
        )
    }

    private fun weatherDto(
        timestamps: List<Long>
    ): WeatherDto = WeatherDto(
        latitude = 42.7,
        longitude = 23.3,
        elevation = 550.0,
        timezone = "Europe/Sofia",
        hourlyWeather = hourlyWeather(timestamps)
    )

    @Test
    fun unixSecondsAreConvertedToMilliseconds() {
        val timestampSeconds = 1_700_000_000L

        val entities = mapper.weatherDtoToEntity(
            weatherDto = weatherDto(listOf(timestampSeconds)),
            vineyardId = vineyardId,
            latestCompletedHourStart = 1_800_000_000_000L
        )

        assertEquals(1, entities.size)
        assertEquals(
            1_700_000_000_000L,
            entities.single().timestamp
        )
    }

    @Test
    fun completedHoursAreIncluded() {
        val entities = mapper.weatherDtoToEntity(
            weatherDto = weatherDto(
                listOf(
                    1_700_000_000L,
                    1_700_003_600L
                )
            ),
            vineyardId = vineyardId,
            latestCompletedHourStart = 1_700_003_600_000L
        )

        assertEquals(2, entities.size)
    }

    @Test
    fun incompleteAndFutureHoursAreExcluded() {
        val entities = mapper.weatherDtoToEntity(
            weatherDto = weatherDto(
                listOf(
                    1_700_000_000L,
                    1_700_003_600L,
                    1_700_007_200L
                )
            ),
            vineyardId = vineyardId,
            latestCompletedHourStart = 1_700_000_000_000L
        )

        assertEquals(1, entities.size)
        assertEquals(
            1_700_000_000_000L,
            entities.single().timestamp
        )
    }

    @Test
    fun weatherMeasurementsAreMappedCorrectly() {
        val entities = mapper.weatherDtoToEntity(
            weatherDto = weatherDto(listOf(1_700_000_000L)),
            vineyardId = vineyardId,
            latestCompletedHourStart = 1_800_000_000_000L
        )

        val entity = entities.single()

        assertEquals(21.5, entity.hourlyTemp2m)
        assertEquals(75.0, entity.relativeHumidity2m)
        assertEquals(16.0, entity.dewPoint2m)
        assertEquals(40.0, entity.cloudCover)
        assertEquals(8.0, entity.windSpeed10m)
        assertEquals(1.2, entity.precipitation)
        assertEquals(1.0, entity.rain)
        assertEquals(0.2, entity.showers)
        assertEquals(0.0, entity.snowfall)
        assertEquals(0.0, entity.snowDepth)
        assertEquals(61, entity.weatherCode)
        assertEquals(3000.0, entity.freezingLevelHeight)
        assertEquals(10000.0, entity.visibility)
        assertEquals(19.0, entity.soilTemperature0cm)
        assertEquals(18.0, entity.soilTemperature6cm)
        assertEquals(0.25, entity.soilMoisture0to1cm)
        assertEquals(0.30, entity.soilMoisture1to3cm)
        assertTrue(entity.isDay)
    }

    @Test
    fun emptyHourlyDataReturnsEmptyList() {
        val entities = mapper.weatherDtoToEntity(
            weatherDto = weatherDto(emptyList()),
            vineyardId = vineyardId,
            latestCompletedHourStart = 1_800_000_000_000L
        )

        assertTrue(entities.isEmpty())
    }

    @Test
    fun vineyardIdIsAssignedToEveryEntity() {
        val entities = mapper.weatherDtoToEntity(
            weatherDto = weatherDto(
                listOf(
                    1_700_000_000L,
                    1_700_003_600L
                )
            ),
            vineyardId = vineyardId,
            latestCompletedHourStart = 1_800_000_000_000L
        )

        assertEquals(2, entities.size)
        assertTrue(entities.all { it.vineyardId == vineyardId })
        assertTrue(entities.all { it.weatherId == 0L })
    }
}
