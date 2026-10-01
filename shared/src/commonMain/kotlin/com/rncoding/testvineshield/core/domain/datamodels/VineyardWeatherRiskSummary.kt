package com.rncoding.testvineshield.core.domain.datamodels

import kotlinx.datetime.LocalDate

data class VineyardWeatherRiskSummary(
    val calculatedAt: LocalDate,
    val infectionScore: Int,
    val powderyMildewInitialInfection: Boolean,
    val wetnessHoursAt10C: Int,
    val hoursAt21C: Int,
    val daysFromBudBurst: Int
)