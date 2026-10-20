
package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.disease_risk.VineyardDiseaseRiskEvaluator
import com.rncoding.testvineshield.core.domain.repository.WeatherHistoryRepository

class RefreshWeatherHistoryAndEvaluateRiskUseCase(
    private val weatherHistoryRepository: WeatherHistoryRepository,
    private val vineyardDiseaseRiskEvaluator: VineyardDiseaseRiskEvaluator
) {

    suspend operator fun invoke(
        vineyardId: Long,
        latitude: Double,
        longitude: Double
    ): RefreshWeatherHistoryAndEvaluateRiskResult {

        require(vineyardId > 0L) {
            "Vineyard ID must be positive."
        }

        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "Latitude must be between -90 and 90."
        }

        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Longitude must be between -180 and 180."
        }

        // 1. Fetch and persist weather history.
        // If this fails, disease-risk evaluation must not run.
        val weatherResult =
            weatherHistoryRepository.refreshWeatherHistory(
                vineyardId = vineyardId,
                latitude = latitude,
                longitude = longitude
            )

        // 2. Evaluate after successful persistence.
        //
        // Intentionally run even if insertedHours == 0:
        // previously failed evaluations may need recovery.
        val diseaseRiskResult =
            vineyardDiseaseRiskEvaluator.evaluateVineyard(
                vineyardId = vineyardId,
                calculatedAt = weatherResult.fetchedAt
            )

        // 3. Return both outcomes.
        return RefreshWeatherHistoryAndEvaluateRiskResult(
            weather = weatherResult,
            diseaseRisk = diseaseRiskResult
        )
    }
}
