package com.rncoding.testvineshield.core.domain.disease_risk.powdery

import kotlinx.datetime.LocalDate

data class PowderyMildewDailyTemperature(
    val date: LocalDate,
    val hourlyTemperaturesCelsius: List<Double>
)