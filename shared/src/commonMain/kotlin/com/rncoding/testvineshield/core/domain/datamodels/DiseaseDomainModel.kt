package com.rncoding.testvineshield.core.domain.datamodels


data class DiseaseDomainModel(
    val diseaseId: Long,
    val code: String?,
    val name: String,
    val description: String,
    val vectors: String,
    val factors: String,
    val treatmentSuggestion: String
)
