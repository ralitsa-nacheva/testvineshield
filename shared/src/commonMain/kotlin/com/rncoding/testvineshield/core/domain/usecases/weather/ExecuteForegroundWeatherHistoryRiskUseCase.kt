package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.scheduler.ForegroundWeatherOutcome
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.startup.AppInitializer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Initialization and ownership checks happen on every attempt, including retries. */
class ExecuteForegroundWeatherHistoryRiskUseCase(
    private val initializer: AppInitializer,
    private val authorize: AuthorizeWeatherHistoryBackgroundWorkUseCase,
    private val refresh: RefreshWeatherHistoryAndEvaluateRiskUseCase
) {
    suspend operator fun invoke(userId: Long, vineyardId: Long): ForegroundWeatherOutcome {
        initializer.ensureInitialized()
        currentCoroutineContext().ensureActive()
        val authorization = authorize(userId, vineyardId)
        currentCoroutineContext().ensureActive()
        if (authorization !is WeatherHistoryWorkAuthorization.Authorized) {
            return ForegroundWeatherOutcome.REMOVE
        }
        val location = authorization.location
        return when (val result = refresh(vineyardId, location.latitude, location.longitude)) {
            is Result.Success -> if (result.data.hasPartialFailures) {
                ForegroundWeatherOutcome.PARTIAL_FAILURE
            } else ForegroundWeatherOutcome.SUCCESS
            is Result.Error -> if (WeatherHistoryRetryPolicy.shouldRetry(result.error)) {
                ForegroundWeatherOutcome.RETRY
            } else ForegroundWeatherOutcome.FAILED
        }
    }
}
