package com.rncoding.testvineshield.core.domain.datamodels

import kotlinx.datetime.LocalDate

data class CompletedBlockActivity(
    val activityId: Long,
    val activityType: String,
    val description: String,
    val completedOn: LocalDate
)

data class LinkedObservationActivity(
    val occurrenceId: Long,
    val activityId: Long,
    val activityType: String,
    val description: String,
    val appliedOn: LocalDate
)
