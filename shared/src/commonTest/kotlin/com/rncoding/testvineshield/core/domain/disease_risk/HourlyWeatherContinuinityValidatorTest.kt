package com.rncoding.testvineshield.core.domain.disease_risk

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HourlyWeatherContinuityValidatorTest {

    private val oneHourMillis =
        HourlyWeatherContinuityValidator.ONE_HOUR_MILLIS

    private fun weatherPoint(timestamp: Long): DiseaseRiskWeatherPoint {
        return DiseaseRiskWeatherPoint(
            timestamp = timestamp,
            temperatureCelsius = 20.0,
            relativeHumidityPercent = 75.0,
            dewPointCelsius = 15.0,
            precipitationMm = 0.0,
            soilMoisture0To1Cm = null,
            soilMoisture1To3Cm = null,
            leafWetness = null,
            isDay = true
        )
    }

    private fun hourlyWeather(
        startTimestamp: Long,
        hours: Int
    ): List<DiseaseRiskWeatherPoint> {
        return List(hours) { index ->
            weatherPoint(
                startTimestamp + index.toLong() * oneHourMillis
            )
        }
    }

    private fun localDayWindow(
        year: Int,
        month: Int,
        day: Int,
        nextYear: Int,
        nextMonth: Int,
        nextDay: Int,
        timeZone: String
    ): DiseaseRiskWeatherWindow {
        val zone = TimeZone.of(timeZone)

        val start = LocalDate(year, month, day)
            .atStartOfDayIn(zone)
            .toEpochMilliseconds()

        val end = LocalDate(nextYear, nextMonth, nextDay)
            .atStartOfDayIn(zone)
            .toEpochMilliseconds()

        return DiseaseRiskWeatherWindow(
            startTimestamp = start,
            endTimestamp = end
        )
    }

    @Test
    fun normal24HourWindowIsComplete() {
        val window = localDayWindow(
            2026, 6, 15,
            2026, 6, 16,
            "Europe/Sofia"
        )

        val weather = hourlyWeather(window.startTimestamp, 24)

        assertNull(
            HourlyWeatherContinuityValidator.validateWindowCompleteness(
                weather,
                window
            )
        )

        assertTrue(
            HourlyWeatherContinuityValidator.isStrictlyContinuous(weather)
        )
    }

    @Test
    fun springDst23HourWindowIsComplete() {
        val window = localDayWindow(
            2026, 3, 29,
            2026, 3, 30,
            "Europe/Sofia"
        )

        val weather = hourlyWeather(window.startTimestamp, 23)

        assertEquals(
            23L * oneHourMillis,
            window.endTimestamp - window.startTimestamp
        )

        assertNull(
            HourlyWeatherContinuityValidator.validateWindowCompleteness(
                weather,
                window
            )
        )
    }

    @Test
    fun autumnDst25HourWindowIsComplete() {
        val window = localDayWindow(
            2026, 10, 25,
            2026, 10, 26,
            "Europe/Sofia"
        )

        val weather = hourlyWeather(window.startTimestamp, 25)

        assertEquals(
            25L * oneHourMillis,
            window.endTimestamp - window.startTimestamp
        )

        assertNull(
            HourlyWeatherContinuityValidator.validateWindowCompleteness(
                weather,
                window
            )
        )
    }

    @Test
    fun missingMiddleObservationIsRejected() {
        val start = 1_000L * oneHourMillis

        val window = DiseaseRiskWeatherWindow(
            startTimestamp = start,
            endTimestamp = start + 24L * oneHourMillis
        )

        val weather = hourlyWeather(start, 24)
            .filterIndexed { index, _ -> index != 12 }

        val error =
            HourlyWeatherContinuityValidator.validateWindowCompleteness(
                weather,
                window
            )

        assertEquals(
            "Expected 24 hourly observations, but received 23.",
            error
        )
    }

    @Test
    fun duplicateTimestampIsRejected() {
        val start = 1_000L * oneHourMillis

        val weather = hourlyWeather(start, 24).toMutableList()

        weather[12] = weatherPoint(weather[11].timestamp)

        val window = DiseaseRiskWeatherWindow(
            startTimestamp = start,
            endTimestamp = start + 24L * oneHourMillis
        )

        assertEquals(
            "Weather observations are not strictly continuous.",
            HourlyWeatherContinuityValidator.validateWindowCompleteness(
                weather,
                window
            )
        )

        assertFalse(
            HourlyWeatherContinuityValidator.isStrictlyContinuous(weather)
        )
    }

    @Test
    fun unorderedObservationsAreRejected() {
        val start = 1_000L * oneHourMillis

        val weather = hourlyWeather(start, 24).toMutableList()

        val temporary = weather[10]
        weather[10] = weather[11]
        weather[11] = temporary

        val window = DiseaseRiskWeatherWindow(
            startTimestamp = start,
            endTimestamp = start + 24L * oneHourMillis
        )

        assertEquals(
            "Weather observations are not strictly continuous.",
            HourlyWeatherContinuityValidator.validateWindowCompleteness(
                weather,
                window
            )
        )

        assertFalse(
            HourlyWeatherContinuityValidator.isStrictlyIncreasing(weather)
        )
    }

    @Test
    fun incorrectFirstTimestampIsRejected() {
        val start = 1_000L * oneHourMillis

        val window = DiseaseRiskWeatherWindow(
            startTimestamp = start,
            endTimestamp = start + 24L * oneHourMillis
        )

        val weather = hourlyWeather(
            start + oneHourMillis,
            24
        )

        assertEquals(
            "First weather observation does not match window start.",
            HourlyWeatherContinuityValidator.validateWindowCompleteness(
                weather,
                window
            )
        )
    }

    @Test
    fun incorrectLastTimestampIsRejected() {
        val start = 1_000L * oneHourMillis

        val window = DiseaseRiskWeatherWindow(
            startTimestamp = start,
            endTimestamp = start + 24L * oneHourMillis
        )

        val weather = hourlyWeather(start, 24).toMutableList()

        weather[23] = weatherPoint(
            start + 24L * oneHourMillis
        )

        assertEquals(
            "Last weather observation does not match window end.",
            HourlyWeatherContinuityValidator.validateWindowCompleteness(
                weather,
                window
            )
        )
    }

    @Test
    fun emptyWindowIsRejected() {
        val start = 1_000L * oneHourMillis

        val window = DiseaseRiskWeatherWindow(
            startTimestamp = start,
            endTimestamp = start
        )

        assertEquals(
            "Weather window is empty.",
            HourlyWeatherContinuityValidator.validateWindowCompleteness(
                emptyList(),
                window
            )
        )
    }

    @Test
    fun partialHourWindowIsRejected() {
        val start = 1_000L * oneHourMillis

        val window = DiseaseRiskWeatherWindow(
            startTimestamp = start,
            endTimestamp = start + 90L * 60_000L
        )

        assertEquals(
            "Weather window must contain complete hourly intervals.",
            HourlyWeatherContinuityValidator.validateWindowCompleteness(
                hourlyWeather(start, 1),
                window
            )
        )
    }
}