package com.rncoding.testvineshield.core.domain.disease_risk

object DiseaseRiskConfidenceEvaluator {

    fun fromLeafWetness(
        weather: List<DiseaseRiskWeatherPoint>
    ): DiseaseRiskConfidence {

        val measurements =
            weather.mapNotNull {
                it.leafWetness
            }

        if (measurements.isEmpty()) {
            return DiseaseRiskConfidence.LOW
        }

        if (
            measurements.any {
                it.value == null
            }
        ) {
            return DiseaseRiskConfidence.LOW
        }

        val minimumConfidence =
            measurements.minOf {
                confidenceRank(
                    it.confidence
                )
            }

        return when (minimumConfidence) {

            HIGH_CONFIDENCE_RANK ->
                DiseaseRiskConfidence.HIGH

            MODERATE_CONFIDENCE_RANK ->
                DiseaseRiskConfidence.MODERATE

            else ->
                DiseaseRiskConfidence.LOW
        }
    }

    private fun confidenceRank(
        confidence: MeasurementConfidence
    ): Int {

        return when (confidence) {
            MeasurementConfidence.LOW ->
                LOW_CONFIDENCE_RANK

            MeasurementConfidence.MODERATE ->
                MODERATE_CONFIDENCE_RANK

            MeasurementConfidence.HIGH ->
                HIGH_CONFIDENCE_RANK
        }
    }

    private const val LOW_CONFIDENCE_RANK = 1
    private const val MODERATE_CONFIDENCE_RANK = 2
    private const val HIGH_CONFIDENCE_RANK = 3
}