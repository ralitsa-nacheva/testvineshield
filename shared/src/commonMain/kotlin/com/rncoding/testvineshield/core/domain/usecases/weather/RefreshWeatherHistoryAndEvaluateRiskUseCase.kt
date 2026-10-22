
package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.disease_risk.VineyardDiseaseRiskEvaluator
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.WeatherHistoryError
import com.rncoding.testvineshield.core.domain.repository.WeatherHistoryRepository
import kotlinx.coroutines.CancellationException

class RefreshWeatherHistoryAndEvaluateRiskUseCase(
    private val weatherHistoryRepository: WeatherHistoryRepository,
    private val vineyardDiseaseRiskEvaluator: VineyardDiseaseRiskEvaluator
) {

    suspend operator fun invoke(
        vineyardId: Long,
        latitude: Double,
        longitude: Double
    ): Result<RefreshWeatherHistoryAndEvaluateRiskResult, AppError> {

        if (vineyardId <= 0L) {
            return Result.Error(
                WeatherHistoryError.InvalidInput(
                    "Vineyard ID must be positive."
                )
            )
        }

        if (!latitude.isFinite() || latitude !in -90.0..90.0) {
            return Result.Error(
                WeatherHistoryError.InvalidInput(
                    "Latitude must be between -90 and 90."
                )
            )
        }

        if (!longitude.isFinite() || longitude !in -180.0..180.0) {
            return Result.Error(
                WeatherHistoryError.InvalidInput(
                    "Longitude must be between -180 and 180."
                )
            )
        }

        val weatherResult = when (
            val result = weatherHistoryRepository.refreshWeatherHistory(
                vineyardId = vineyardId,
                latitude = latitude,
                longitude = longitude
            )
        ) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }

        val diseaseRiskResult = try {
            vineyardDiseaseRiskEvaluator.evaluateVineyard(
                vineyardId = vineyardId,
                calculatedAt = weatherResult.fetchedAt
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return Result.Error(
                WeatherHistoryError.EvaluationFailed(exception)
            )
        }

        return Result.Success(
            RefreshWeatherHistoryAndEvaluateRiskResult(
                weather = weatherResult,
                diseaseRisk = diseaseRiskResult
            )
        )
    }
}
