package com.rncoding.testvineshield.core.domain.disease_risk.downy

data class DownyMildewIncubationPrediction(
    val infectionTimestamp: Long,
    val earliestExpectedSymptomsAt: Long,
    val latestExpectedSymptomsAt: Long,
    val confidence: Confidence
) {

    enum class Confidence {
        LOW,
        MODERATE
    }
}