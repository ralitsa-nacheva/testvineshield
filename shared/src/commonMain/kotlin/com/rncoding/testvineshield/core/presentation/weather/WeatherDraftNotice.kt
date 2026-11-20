package com.rncoding.testvineshield.core.presentation.weather

import com.rncoding.testvineshield.core.domain.disease_risk.MonitoringDraftSaveOutcome
import com.rncoding.testvineshield.core.domain.error.AppError

/** Language-independent result retained until the UI renders it. */
sealed interface WeatherDraftNotice {
    data class Outcome(val value: MonitoringDraftSaveOutcome) : WeatherDraftNotice
    data class Failure(val error: AppError) : WeatherDraftNotice
}
