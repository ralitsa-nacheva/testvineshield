
package com.rncoding.testvineshield.core.worker

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rncoding.testvineshield.core.domain.scheduler.WeatherHistoryRiskScheduler
import java.util.concurrent.TimeUnit

class WeatherHistoryRiskWorkScheduler(
    private val workManager: WorkManager
) : WeatherHistoryRiskScheduler {

    override fun schedule(
        userId: Long,
        vineyardId: Long
    ) {
        require(userId > 0L) {
            "User ID must be positive."
        }
        require(vineyardId > 0L) {
            "Vineyard ID must be positive."
        }

        val inputData = Data.Builder()
            .putLong(
                WeatherHistoryRiskWorker.KEY_USER_ID,
                userId
            )
            .putLong(
                WeatherHistoryRiskWorker.KEY_VINEYARD_ID,
                vineyardId
            )
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                NetworkType.CONNECTED
            )
            .build()

        val request =
            PeriodicWorkRequestBuilder<WeatherHistoryRiskWorker>(
                1,
                TimeUnit.HOURS
            )
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .addTag(userTag(userId))
                .addTag(vineyardTag(userId, vineyardId))
                .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName(userId, vineyardId),
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun cancel(
        userId: Long,
        vineyardId: Long
    ) {
        require(userId > 0L)
        require(vineyardId > 0L)

        workManager.cancelUniqueWork(
            uniqueWorkName(userId, vineyardId)
        )
    }

    override fun cancelAllForUser(
        userId: Long
    ) {
        require(userId > 0L)

        workManager.cancelAllWorkByTag(
            userTag(userId)
        )
    }

    private fun uniqueWorkName(
        userId: Long,
        vineyardId: Long
    ): String =
        "weather_history_risk_user_${userId}_vineyard_$vineyardId"

    private fun userTag(
        userId: Long
    ): String =
        "weather_history_risk_user_$userId"

    private fun vineyardTag(
        userId: Long,
        vineyardId: Long
    ): String =
        "weather_history_risk_vineyard_${userId}_$vineyardId"
}
