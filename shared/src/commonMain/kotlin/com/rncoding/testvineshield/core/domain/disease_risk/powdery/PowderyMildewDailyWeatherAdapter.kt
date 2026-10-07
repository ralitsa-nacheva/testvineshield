package com.rncoding.testvineshield.core.domain.disease_risk.powdery

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object PowderyMildewDailyWeatherAdapter {

    fun map(
        weather: List<DiseaseRiskWeatherPoint>,
        timeZoneId: String
    ): List<PowderyMildewDailyTemperature> {

        if (weather.isEmpty()) {
            return emptyList()
        }

        val timeZone =
            TimeZone.of(
                timeZoneId
            )

        return weather
            .sortedBy {
                it.timestamp
            }
            .groupBy { point ->
                Instant
                    .fromEpochMilliseconds(
                        point.timestamp
                    )
                    .toLocalDateTime(
                        timeZone
                    )
                    .date
            }
            .map { (date, points) ->
                PowderyMildewDailyTemperature(
                    date = date,
                    hourlyTemperaturesCelsius =
                        points.map {
                            it.temperatureCelsius
                        }
                )
            }
            .sortedBy {
                it.date
            }
    }
}