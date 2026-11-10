package com.rncoding.testvineshield.core.presentation.weather

import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.usecases.weather.VineyardWeatherSnapshot
import kotlinx.datetime.LocalDate
import kotlin.test.*

class WeatherPresentationTest {
    private val hour = 3_600_000L
    private val now = 1_793_448_000_000L
    private val vineyard = VineyardDomainModel(10, 1, "North", 1.0, "BG", "Plovdiv", 42.1, 24.7, "Europe/Sofia", null, 0, LocalDate(2026,1,1), LocalDate(2026,1,1))
    private fun weather(at: Long) = WeatherHistoryDomainModel(10, at, 18.0, 60.0, 10.0, 20.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 2000.0, 10000.0, 12.0, 10.0, 0.3, 0.3, true)
    private fun snapshot(past: List<WeatherHistoryDomainModel> = emptyList(), future: List<CachedForecastHour> = emptyList()) = VineyardWeatherSnapshot(vineyard, past, future, emptyList(), emptyList())
    @Test fun emptyCachesRemainUnknown() {
        val p = presentWeather(snapshot(), now)
        assertEquals(CacheAge.EMPTY, p.historyAge); assertEquals(CacheAge.EMPTY, p.forecastAge)
        assertNull(p.forecastFetchedAt); assertNull(p.historyCoverage.first)
    }
    @Test fun historyExcludesIncompleteAndFutureHoursAndCountsDiscontinuities() {
        val p = presentWeather(snapshot(listOf(weather(now - 4*hour), weather(now - 2*hour), weather(now - hour), weather(now), weather(now + hour))), now)
        assertEquals(3, p.history.size); assertEquals(2, p.excludedHistoryHours)
        assertEquals(1, p.historyCoverage.discontinuities); assertEquals(CacheAge.INVALID_CLOCK, p.historyAge)
        assertEquals(now - hour, p.history.last().timestamp)
    }
    @Test fun ageChangesWithoutNewRepositoryEmissionsAndThresholdsAreExplicit() {
        val s = snapshot(listOf(weather(now - hour)), listOf(CachedForecastHour(weather(now + 24*hour), now)))
        assertEquals(CacheAge.RECENT, presentWeather(s, now).historyAge)
        assertEquals(CacheAge.RECENT, presentWeather(s, now).forecastAge)
        assertEquals(CacheAge.STALE, presentWeather(s, now + 3*hour).historyAge)
        assertEquals(CacheAge.STALE, presentWeather(s, now + 13*hour).forecastAge)
    }
    @Test fun validForecastTimeDoesNotDisguiseOldFetchOrExpiredHorizon() {
        assertEquals(CacheAge.STALE, presentWeather(snapshot(future = listOf(CachedForecastHour(weather(now + hour), now - 13*hour))), now).forecastAge)
        val p = presentWeather(snapshot(future = listOf(CachedForecastHour(weather(now - hour), now))), now)
        assertEquals(CacheAge.STALE, p.forecastAge); assertTrue(p.forecast.isEmpty())
    }
    @Test fun mixedAndFutureFetchTimesAreNotRecent() {
        val mixed = presentWeather(snapshot(future = listOf(CachedForecastHour(weather(now + hour), now), CachedForecastHour(weather(now + 2*hour), now-1))), now)
        assertTrue(mixed.mixedForecastFetchTimes); assertNull(mixed.forecastFetchedAt)
        assertEquals(CacheAge.INVALID_CLOCK, mixed.forecastAge)
        assertEquals(CacheAge.INVALID_CLOCK, presentWeather(snapshot(future = listOf(CachedForecastHour(weather(now+hour), now+1))), now).forecastAge)
    }
    @Test fun invalidMetricsAreNotRenderedAsNumbers() {
        assertEquals("Unavailable", weatherMetric(Double.NaN, "°C"))
        assertEquals("Unavailable", weatherMetric(Double.POSITIVE_INFINITY, "mm"))
        assertEquals("Unavailable", weatherMetric(-1.0, "%", 0.0..100.0))
        assertEquals("Unavailable", weatherMetric(101.0, "%", 0.0..100.0))
        assertEquals("0.0 mm", weatherMetric(0.0, "mm", 0.0..Double.MAX_VALUE))
    }
    @Test fun repeatedLocalHoursRetainDifferentOffsetsAndBadZoneFallsBackHonestly() {
        val first = kotlin.time.Instant.parse("2026-10-25T00:30:00Z").toEpochMilliseconds()
        val second = kotlin.time.Instant.parse("2026-10-25T01:30:00Z").toEpochMilliseconds()
        val a = weatherTime(first, "Europe/Sofia"); val b = weatherTime(second, "Europe/Sofia")
        assertTrue(a.contains("03:30")); assertTrue(b.contains("03:30")); assertNotEquals(a,b)
        assertTrue(weatherTime(first,"Invalid/Zone").contains("UTC; vineyard time zone unavailable"))
        assertEquals("Unknown", weatherTime(null, "Europe/Sofia"))
    }
    @Test fun historyWindowRollsInElapsedHoursWithoutPretendingCalendarDays() {
        val p = presentWeather(snapshot(listOf(weather(now - 169*hour), weather(now - 168*hour), weather(now - hour))), now)
        assertEquals(2, p.historyCoverage.count); assertEquals(now - 168*hour, p.historyCoverage.first)
        assertEquals(1, p.historyCoverage.discontinuities)
    }
}
