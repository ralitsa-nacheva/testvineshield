
package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.remote.dto.HourlyWeatherDto
import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeatherHistoryMapperTest {

    private val mapper = WeatherHistoryMapper()
    private val hourMillis = 3_600_000L

    @Test
    fun preservesHalfHourUtcOffset() {
        val firstTimestamp = 1_792_089_000L * 1_000L

        val result = mapper.weatherDtoToEntity(
            weatherDto = weatherDto(
                listOf(
                    firstTimestamp / 1_000L,
                    firstTimestamp / 1_000L + 3_600L
                )
            ),
            vineyardId = 1L,
            latestCompletedHourStart = firstTimestamp + hourMillis
        )

        assertEquals(2, result.size)
        assertEquals(firstTimestamp, result[0].timestamp)
        assertEquals(
            1_800_000L,
            result[0].timestamp % hourMillis
        )
        assertEquals(
            hourMillis,
            result[1].timestamp - result[0].timestamp
        )
    }

    @Test
    fun excludesIncompleteAndFutureHours() {
        val firstTimestamp = 1_792_089_000L * 1_000L

        val result = mapper.weatherDtoToEntity(
            weatherDto = weatherDto(
                listOf(
                    firstTimestamp / 1_000L,
                    firstTimestamp / 1_000L + 3_600L,
                    firstTimestamp / 1_000L + 7_200L,
                    firstTimestamp / 1_000L + 10_800L
                )
            ),
            vineyardId = 1L,
            latestCompletedHourStart = firstTimestamp + hourMillis
        )

        assertEquals(2, result.size)
        assertEquals(
            listOf(firstTimestamp, firstTimestamp + hourMillis),
            result.map { it.timestamp }
        )
    }

    @Test
    fun retainsOlderHoursForGapRecovery() {
        val timestamps = listOf(
            1_792_089_000L,
            1_792_092_600L,
            1_792_096_200L,
            1_792_099_800L
        )

        val result = mapper.weatherDtoToEntity(
            weatherDto = weatherDto(timestamps),
            vineyardId = 7L,
            latestCompletedHourStart = timestamps.last() * 1_000L
        )

        assertEquals(4, result.size)
        assertTrue(result.all { it.vineyardId == 7L })
        assertEquals(
            timestamps.map { it * 1_000L },
            result.map { it.timestamp }
        )
    }

    private fun weatherDto(
        timestamps: List<Long>
    ): WeatherDto {

        val count = timestamps.size
        val doubles = List(count) { 1.0 }
        val integers = List(count) { 0 }
        val booleans = List(count) { true }

        return WeatherDto(
            latitude = 28.6,
            longitude = 77.2,
            elevation = 200.0,
            timezone = "Asia/Kolkata",
            hourlyWeather = HourlyWeatherDto(
                timestamp = timestamps,
                hourlyTemp2m = doubles,
                relativeHumidity2m = doubles,
                dewPoint2m = doubles,
                cloudCover = doubles,
                windSpeed10m = doubles,
                precipitation = doubles,
                rain = doubles,
                showers = doubles,
                snowfall = doubles,
                snowDepth = doubles,
                weatherCode = integers,
                freezingLevelHeight = doubles,
                visibility = doubles,
                soilTemperature0cm = doubles,
                soilTemperature6cm = doubles,
                soilMoisture0to1cm = doubles,
                soilMoisture1to3cm = doubles,
                isDay = booleans
            )
        )
    }
}
