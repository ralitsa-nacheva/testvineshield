package com.rncoding.testvineshield.core.domain.disease_risk.wetness

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint
import com.rncoding.testvineshield.core.domain.disease_risk.EnvironmentalMeasurement

interface LeafWetnessEstimator {

    fun estimate(
        weather: DiseaseRiskWeatherPoint
    ): EnvironmentalMeasurement<Boolean>
}