package com.rncoding.testvineshield.core.data.remote

import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto
import kotlin.test.*

class WeatherResponseValidatorTest {
    private val good get() = WeatherResponseFixtures.response()
    private fun rejected(dto: WeatherDto) { assertFailsWith<IllegalArgumentException> { WeatherResponseValidator.validate(dto) } }
    @Test fun validatesAllRequestedUnitsAndAllowsExtraMetadata() {
        WeatherResponseValidator.validate(good.copy(hourlyUnits = good.hourlyUnits!! + ("unused" to "extra")))
        for ((name,_) in WeatherResponseValidator.expectedUnits) {
            rejected(good.copy(hourlyUnits = good.hourlyUnits!! - name))
            rejected(good.copy(hourlyUnits = good.hourlyUnits!! + (name to "unsupported")))
        }
    }
    @Test fun emptyOversizedAndUnequalArraysFailBeforeFiltering() {
        rejected(WeatherResponseFixtures.response(0))
        rejected(WeatherResponseFixtures.response(WeatherResponseValidator.MAX_RESPONSE_HOURS + 1))
        val h = good.hourlyWeather
        rejected(good.copy(hourlyWeather = h.copy(hourlyTemp2m = h.hourlyTemp2m.dropLast(1))))
        rejected(good.copy(hourlyWeather = h.copy(isDay = emptyList())))
        rejected(good.copy(hourlyWeather = h.copy(weatherCode = emptyList())))
        rejected(good.copy(hourlyWeather = h.copy(soilTemperature6cm = emptyList())))
    }
    @Test fun duplicateReversedAndMissingHoursAreRejectedWithoutRoundingOffsets() {
        val h = good.hourlyWeather
        rejected(good.copy(hourlyWeather = h.copy(timestamp = h.timestamp.toMutableList().also { it[1] = it[0] })))
        rejected(good.copy(hourlyWeather = h.copy(timestamp = h.timestamp.reversed())))
        rejected(good.copy(hourlyWeather = h.copy(timestamp = h.timestamp.mapIndexed { index,v -> if (index > 0) v+3600 else v })))
        WeatherResponseValidator.validate(WeatherResponseFixtures.response(startSeconds = 1_792_089_000L))
    }
    @Test fun invalidEpochSecondsAndOverflowFailBeforeMultiplication() {
        for (seconds in listOf(0L,-1L,Long.MAX_VALUE,Long.MAX_VALUE/1000 + 1)) {
            rejected(good.copy(hourlyWeather = good.hourlyWeather.copy(timestamp = List(48) { seconds })))
        }
    }
    @Test fun nonfiniteAndOutOfDomainValuesAreRejected() {
        val h = good.hourlyWeather
        for (invalid in listOf(Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY)) {
            rejected(good.copy(hourlyWeather = h.copy(hourlyTemp2m = List(48) { invalid })))
            rejected(good.copy(hourlyWeather = h.copy(soilMoisture1to3cm = List(48) { invalid })))
        }
        rejected(good.copy(hourlyWeather = h.copy(relativeHumidity2m = List(48) { 101.0 })))
        rejected(good.copy(hourlyWeather = h.copy(cloudCover = List(48) { -0.1 })))
        rejected(good.copy(hourlyWeather = h.copy(soilMoisture0to1cm = List(48) { 1.1 })))
        rejected(good.copy(hourlyWeather = h.copy(precipitation = List(48) { -0.1 })))
        rejected(good.copy(hourlyWeather = h.copy(windSpeed10m = List(48) { -0.1 })))
        rejected(good.copy(hourlyWeather = h.copy(hourlyTemp2m = List(48) { -274.0 })))
        rejected(good.copy(hourlyWeather = h.copy(weatherCode = List(48) { 999 })))
    }
    @Test fun gridCellCoordinatesMayDifferButInvalidMetadataIsRejected() {
        WeatherResponseValidator.validate(good.copy(latitude=42.8,longitude=23.4,elevation=-50.0))
        rejected(good.copy(latitude=Double.NaN)); rejected(good.copy(longitude=181.0)); rejected(good.copy(elevation=Double.POSITIVE_INFINITY))
        assertFailsWith<Exception> { WeatherResponseValidator.validate(good.copy(timezone="Invalid/Zone")) }
    }
}
