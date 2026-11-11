package com.rncoding.testvineshield.core.presentation.weather

import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.usecases.weather.ObserveOwnedWeatherUseCase
import com.rncoding.testvineshield.core.domain.usecases.weather.VineyardWeatherSnapshot
import com.rncoding.testvineshield.core.domain.disease_risk.previousCompleteVineyardDay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.offsetAt
import kotlin.time.Instant

enum class CacheAge { EMPTY, RECENT, STALE, INVALID_CLOCK }
data class HourCoverage(val count: Int, val first: Long?, val last: Long?, val discontinuities: Int)
data class WeatherPresentation(
    val history: List<WeatherHistoryDomainModel>,
    val historyAge: CacheAge,
    val historyCoverage: HourCoverage,
    val excludedHistoryHours: Int,
    val forecast: List<CachedForecastHour>,
    val forecastAge: CacheAge,
    val forecastFetchedAt: Long?,
    val forecastCoverage: HourCoverage,
    val mixedForecastFetchTimes: Boolean
)

/** Display thresholds describe cache age, never infection risk or agronomic validity. */
fun presentWeather(snapshot: VineyardWeatherSnapshot, now: Long): WeatherPresentation {
    val history = snapshot.history.filter { it.timestamp >= now - ObserveOwnedWeatherUseCase.HISTORY_WINDOW_MILLIS && it.timestamp <= now - HOUR }
        .sortedBy { it.timestamp }
    val latest = history.lastOrNull()?.timestamp
    val excluded = snapshot.history.count { it.timestamp > now - HOUR }
    val historyAge = when {
        excluded > 0 -> CacheAge.INVALID_CLOCK
        latest == null -> CacheAge.EMPTY
        now - latest > 3 * HOUR -> CacheAge.STALE
        else -> CacheAge.RECENT
    }
    // Keep the current validity hour until it ends; the repository skips the partial hour on fetch.
    val future = snapshot.forecast.filter { it.weather.timestamp > now - HOUR }.sortedBy { it.weather.timestamp }
    val fetched = snapshot.forecast.map { it.fetchedAt }.distinct()
    val fetchedAt = fetched.singleOrNull()
    val forecastAge = when {
        snapshot.forecast.isEmpty() -> CacheAge.EMPTY
        fetched.size != 1 || fetchedAt == null || fetchedAt <= 0 || fetchedAt > now -> CacheAge.INVALID_CLOCK
        future.isEmpty() || now - fetchedAt > 12 * HOUR -> CacheAge.STALE
        else -> CacheAge.RECENT
    }
    return WeatherPresentation(history, historyAge, coverage(history.map { it.timestamp }), excluded,
        future, forecastAge, fetchedAt, coverage(future.map { it.weather.timestamp }), fetched.size > 1)
}
internal data class LocalDayCacheCoverage(
    val date: LocalDate,
    val expectedHours: Int?,
    val matchedHours: Int,
    val unexpectedRows: Int
)

/** Count only rows on the same hourly grid as the experimental Botrytis window. */
internal fun previousDayCacheCoverage(snapshot: VineyardWeatherSnapshot, now: Long): LocalDayCacheCoverage? {
    val zone = try {
        TimeZone.of(snapshot.vineyard.timeZone)
    } catch (_: IllegalArgumentException) {
        return null
    }
    val day = previousCompleteVineyardDay(now, zone)
    val (start, end) = day.window
    val duration = end - start
    if (duration <= 0 || duration % HOUR != 0L) {
        return LocalDayCacheCoverage(day.date, null, 0, 0)
    }

    val rows = snapshot.history.asSequence().map { it.timestamp }.filter { it >= start && it < end }.toList()
    val matchedHours = rows.filter { (it - start) % HOUR == 0L }.distinct().size
    return LocalDayCacheCoverage(
        date = day.date,
        expectedHours = (duration / HOUR).toInt(),
        matchedHours = matchedHours,
        unexpectedRows = rows.size - matchedHours
    )
}

private const val HOUR = 3_600_000L
private fun coverage(times: List<Long>) = HourCoverage(times.size, times.firstOrNull(), times.lastOrNull(),
    times.zipWithNext().count { (a, b) -> b - a != HOUR })

/** Includes the UTC offset: repeated local hours at autumn DST remain distinguishable. */
fun weatherTime(timestamp: Long?, zoneId: String): String {
    if (timestamp == null) return "Unknown"
    return try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val zone = TimeZone.of(zoneId)
        "${instant.toLocalDateTime(zone)} ${zone.offsetAt(instant)} ($zoneId)"
    } catch (_: Exception) {
        try { "${Instant.fromEpochMilliseconds(timestamp)} (UTC; vineyard time zone unavailable)" }
        catch (_: Exception) { "Invalid timestamp" }
    }
}

fun weatherMetric(value: Double, unit: String, range: ClosedFloatingPointRange<Double>? = null): String =
    if (!value.isFinite() || (range != null && value !in range)) "Unavailable" else "$value $unit"

fun savedRefreshMessage(code: String): String = when(code) {
    "timeout" -> "The previous request timed out. Retry when ready."
    "network_unavailable" -> "The weather service could not be reached. Retry when ready."
    "invalid_response" -> "The previous response could not be validated; saved weather was retained."
    "interrupted" -> "The previous request was interrupted. Retry when ready."
    "http_429" -> "The weather service rate limit was reached. Try again later."
    else -> "The previous refresh did not complete. Retry when ready."
}
