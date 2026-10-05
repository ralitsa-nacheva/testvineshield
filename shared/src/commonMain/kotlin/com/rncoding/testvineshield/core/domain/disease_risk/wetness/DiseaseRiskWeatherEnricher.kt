package com.rncoding.testvineshield.core.domain.disease_risk.wetness

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint

class DiseaseRiskWeatherEnricher(
    private val leafWetnessEstimator: LeafWetnessEstimator
) {

    fun enrich(
        weather: List<DiseaseRiskWeatherPoint>
    ): List<DiseaseRiskWeatherPoint> {

        return weather.map { point ->

            if (point.leafWetness != null) {
                point
            } else {
                point.copy(
                    leafWetness =
                        leafWetnessEstimator
                            .estimate(point)
                )
            }
        }
    }
}