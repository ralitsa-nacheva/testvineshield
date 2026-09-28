package com.rncoding.testvineshield.core.domain.datamodels

import androidx.room.ColumnInfo

data class ObservedSymptomDomainModel(
    val observationId: Long,
    val symptomId: Long,
    val occurrenceId: Long,
    val observedAt: Long,
    val severity: Int,
    val note: String
)
