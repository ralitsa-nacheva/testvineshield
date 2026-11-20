package com.rncoding.testvineshield.core.presentation.weather

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

/** Resolve fallback text at composition time; timestamp formatting remains pure. */
@Composable
internal fun localizedWeatherTime(timestamp: Long?, zone: String): String = weatherTime(
    timestamp, zone,
    unknown = stringResource(Res.string.weather_time_unknown),
    zoneUnavailable = stringResource(Res.string.weather_zone_unavailable),
    invalidTimestamp = stringResource(Res.string.weather_time_invalid)
)

@Composable
internal fun localizedWeatherMetric(
    value: Double,
    unit: String,
    range: ClosedFloatingPointRange<Double>? = null
): String = weatherMetric(value, unit, range, stringResource(Res.string.weather_value_unavailable))

/** Stored provider-independent codes are not translated or exposed as raw messages. */
@Composable
internal fun localizedSavedRefreshMessage(code: String): String = stringResource(when (code) {
    "timeout" -> Res.string.weather_saved_timeout
    "network_unavailable" -> Res.string.weather_saved_network
    "invalid_response" -> Res.string.weather_saved_invalid
    "interrupted" -> Res.string.weather_saved_interrupted
    "http_429" -> Res.string.weather_saved_rate_limit
    else -> Res.string.weather_saved_incomplete
})

@Composable
internal fun localizedRefreshKind(kind: String): String = stringResource(when (kind) {
    "history" -> Res.string.weather_kind_history
    "forecast" -> Res.string.weather_kind_forecast
    else -> Res.string.weather_kind_unknown
})
