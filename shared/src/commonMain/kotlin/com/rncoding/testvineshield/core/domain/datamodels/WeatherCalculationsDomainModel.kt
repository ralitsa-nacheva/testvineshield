package com.rncoding.testvineshield.core.domain.datamodels

import kotlinx.datetime.LocalDate

data class WeatherCalculationsDomainModel(
    val weatherCalcId: Long,
    val vineyardId: Long,
    val calculatedAt: LocalDate,
    val countWetnessHoursAt10C: Int,
    val countHoursAt21C: Int,
    val powderyMildewInitialInfection: Boolean,
    val daysFromBudBurst: Int,
    val primaryIncubationDaysCountPD: Int,
    val secondaryIncubationDaysCountPD: Int,
    val infectionScore: Int
)
