package com.rncoding.testvineshield.core.domain.disease_risk

data class EnvironmentalMeasurement<T>(
    val value: T?,
    val source: MeasurementSource,
    val confidence: MeasurementConfidence
)