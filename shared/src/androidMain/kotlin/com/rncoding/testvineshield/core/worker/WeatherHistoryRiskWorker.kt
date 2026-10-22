
package com.rncoding.testvineshield.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rncoding.testvineshield.core.domain.usecases.weather.AuthorizeWeatherHistoryBackgroundWorkUseCase
import com.rncoding.testvineshield.core.domain.usecases.weather.RefreshWeatherHistoryAndEvaluateRiskUseCase
import com.rncoding.testvineshield.core.domain.usecases.weather.WeatherHistoryWorkAuthorization
import com.rncoding.testvineshield.core.domain.usecases.weather.WeatherHistoryRetryPolicy
import com.rncoding.testvineshield.core.domain.error.Result as AppResult
import kotlinx.coroutines.CancellationException

class WeatherHistoryRiskWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val authorizeBackgroundWork:
    AuthorizeWeatherHistoryBackgroundWorkUseCase,
    private val refreshWeatherHistoryAndEvaluateRisk:
    RefreshWeatherHistoryAndEvaluateRiskUseCase
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = inputData.getLong(KEY_USER_ID, INVALID_ID)
        val vineyardId = inputData.getLong(KEY_VINEYARD_ID, INVALID_ID)

        if (userId <= 0L || vineyardId <= 0L) {
            return Result.failure()
        }

        return try {
            when (
                val authorization = authorizeBackgroundWork(
                    scheduledUserId = userId,
                    vineyardId = vineyardId
                )
            ) {
                is WeatherHistoryWorkAuthorization.NotAuthorized ->
                    Result.success()

                is WeatherHistoryWorkAuthorization.VineyardNotFound ->
                    Result.success()


                is WeatherHistoryWorkAuthorization.Authorized -> {
                    val location = authorization.location

                    when (
                        val refreshResult = refreshWeatherHistoryAndEvaluateRisk(
                            vineyardId = location.vineyardId,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    ) {
                        is AppResult.Success -> Result.success()

                        is AppResult.Error -> {
                            android.util.Log.e(
                                "WeatherHistoryRiskWorker",
                                "Weather/risk operation failed: " +
                                        refreshResult.error.debugMessage
                            )

                            if (WeatherHistoryRetryPolicy.shouldRetry(refreshResult.error)) {
                                Result.retry()
                            } else {
                                Result.failure()
                            }
                        }
                    }
                }

            }
        }
        catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) { // need a final retry classifier
            android.util.Log.e(
                "WeatherHistoryRiskWorker",
                "Background weather/risk execution failed",
                exception
            )

            Result.failure()
        }

    }

    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_VINEYARD_ID = "vineyard_id"

        private const val INVALID_ID = -1L
    }
}
