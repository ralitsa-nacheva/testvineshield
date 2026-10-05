package com.rncoding.testvineshield.core.domain.disease_risk.wetness

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint
import com.rncoding.testvineshield.core.domain.disease_risk.EnvironmentalMeasurement
import com.rncoding.testvineshield.core.domain.disease_risk.MeasurementConfidence
import com.rncoding.testvineshield.core.domain.disease_risk.MeasurementSource
import kotlin.math.abs

class WeatherBasedLeafWetnessEstimator : LeafWetnessEstimator {

    override fun estimate(
        weather: DiseaseRiskWeatherPoint
    ): EnvironmentalMeasurement<Boolean> {

        val precipitation =
            weather.precipitationMm

        if (
            precipitation != null &&
            precipitation > 0.0
        ) {
            return EnvironmentalMeasurement(
                value = true,
                source = MeasurementSource.ESTIMATED,
                confidence = MeasurementConfidence.MODERATE
            )
        }

        val dewPoint =
            weather.dewPointCelsius

        if (dewPoint != null) {
            val dewPointDepression =
                weather.temperatureCelsius - dewPoint

            if (
                dewPointDepression >= 0.0 &&
                dewPointDepression <=
                MAX_DEW_POINT_DEPRESSION_C
            ) {
                return EnvironmentalMeasurement(
                    value = true,
                    source = MeasurementSource.ESTIMATED,
                    confidence =
                        MeasurementConfidence.LOW
                )
            }
        }

        return EnvironmentalMeasurement(
            value = false,
            source = MeasurementSource.ESTIMATED,
            confidence = MeasurementConfidence.LOW
        )
    }

    companion object {
        const val MAX_DEW_POINT_DEPRESSION_C = 1.0
    }
}