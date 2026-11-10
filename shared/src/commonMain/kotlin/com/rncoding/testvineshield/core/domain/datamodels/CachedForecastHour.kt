package com.rncoding.testvineshield.core.domain.datamodels

/** Forecast validity time and cache fetch time are different facts. */
data class CachedForecastHour(val weather: WeatherHistoryDomainModel, val fetchedAt: Long)
