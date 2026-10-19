package com.rncoding.testvineshield.core.domain.disease_risk.powdery

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

object PowderyMildewDailyWeatherAdapter {

    private const val ONE_HOUR_MILLIS = 3_600_000L

    fun map(
        weather: List<DiseaseRiskWeatherPoint>,
        timeZoneId: String
    ): List<PowderyMildewDailyTemperature> {

        if (weather.isEmpty()) {
            return emptyList()
        }

        val timeZone = TimeZone.of(timeZoneId)

        val grouped = weather
            .sortedBy { it.timestamp }
            .groupBy { point ->
                Instant
                    .fromEpochMilliseconds(point.timestamp)
                    .toLocalDateTime(timeZone)
                    .date
            }

        return grouped.mapNotNull { (date, points) ->

            val startTimestamp = date
                .atStartOfDayIn(timeZone)
                .toEpochMilliseconds()

            val nextDate = date.plus(
                1,
                DateTimeUnit.DAY
            )

            val endTimestamp = nextDate
                .atStartOfDayIn(timeZone)
                .toEpochMilliseconds()

            val expectedHours =
                ((endTimestamp - startTimestamp) /
                        ONE_HOUR_MILLIS).toInt()

            val complete =
                points.size == expectedHours &&
                        points.first().timestamp == startTimestamp &&
                        points.last().timestamp ==
                        endTimestamp - ONE_HOUR_MILLIS &&
                        points.zipWithNext().all { (previous, current) ->
                            current.timestamp - previous.timestamp ==
                                    ONE_HOUR_MILLIS
                        }

            if (!complete) {
                return@mapNotNull null
            }

            PowderyMildewDailyTemperature(
                date = date,
                hourlyTemperaturesCelsius =
                    points.map {
                        it.temperatureCelsius
                    }
            )
        }.sortedBy { it.date }
    }
}