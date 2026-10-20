
package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryRefreshResult
import com.rncoding.testvineshield.core.domain.disease_risk.VineyardDiseaseRiskEvaluationSummary

data class RefreshWeatherHistoryAndEvaluateRiskResult(
    val weather: WeatherHistoryRefreshResult,
    val diseaseRisk: VineyardDiseaseRiskEvaluationSummary
) {
    val vineyardId: Long
        get() = weather.vineyardId

    val insertedWeatherHours: Int
        get() = weather.insertedHours

    val evaluatedConfigurations: Int
        get() = diseaseRisk.total

    val successfulEvaluations: Int
        get() = diseaseRisk.succeeded

    val skippedEvaluations: Int
        get() = diseaseRisk.skipped

    val failedEvaluations: Int
        get() = diseaseRisk.failed

    val hasPartialFailures: Boolean
        get() = diseaseRisk.hasFailures
}
