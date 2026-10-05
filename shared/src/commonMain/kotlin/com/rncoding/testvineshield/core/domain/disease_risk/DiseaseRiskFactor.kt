package com.rncoding.testvineshield.core.domain.disease_risk

data class DiseaseRiskFactor(
    val name: String,
    val value: Double?,
    val unit: String?,
    val satisfied: Boolean?
)