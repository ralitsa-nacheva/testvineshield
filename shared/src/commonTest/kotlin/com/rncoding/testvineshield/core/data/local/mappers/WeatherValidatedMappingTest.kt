package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.remote.WeatherResponseFixtures
import kotlin.test.*

class WeatherValidatedMappingTest {
    @Test fun validatedForecastPreservesOffsetsDayFlagsAndFetchTime() {
        val start=1_792_089_000L
        val dto=WeatherResponseFixtures.response(startSeconds=start)
        val rows=WeatherForecastMapper().weatherDtoToEntities(dto,10,start*1000-1,start*1000)
        assertEquals(48,rows.size); assertEquals(start*1000,rows.first().timestamp)
        assertEquals(1_800_000L,rows.first().timestamp % 3_600_000L)
        assertEquals(start*1000-1,rows.first().forecastCreatedAt)
        assertFalse(rows[0].isDay); assertTrue(rows[1].isDay)
    }
    @Test fun invalidExcludedHourStillRejectsWholeBatch() {
        val dto=WeatherResponseFixtures.response()
        val broken=dto.copy(hourlyWeather=dto.hourlyWeather.copy(hourlyTemp2m=dto.hourlyWeather.hourlyTemp2m.toMutableList().also { it[0]=Double.NaN }))
        assertFailsWith<IllegalArgumentException> { WeatherForecastMapper().weatherDtoToEntities(broken,10,1,Long.MAX_VALUE) }
        assertFailsWith<IllegalArgumentException> { WeatherHistoryMapper().weatherDtoToEntity(broken,10,1) }
    }
    @Test fun validHistoryExcludesIncompleteHoursWithoutInventingMissingWeather() {
        val dto=WeatherResponseFixtures.response()
        val cutoff=dto.hourlyWeather.timestamp[1]*1000
        val rows=WeatherHistoryMapper().weatherDtoToEntity(dto,10,cutoff)
        assertEquals(2,rows.size); assertEquals(cutoff,rows.last().timestamp)
    }
}
