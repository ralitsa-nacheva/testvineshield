package com.rncoding.testvineshield.core.domain.disease_risk.powdery

import kotlinx.datetime.LocalDate

data class PowderyMildewDailyRiskResult(
    val date: LocalDate,
    val riskIndex: Int,
    val qualifyingHours: Int,
    val qualifyingDay: Boolean,
    val highTemperaturePenalty: Boolean,
    val initiated: Boolean
)