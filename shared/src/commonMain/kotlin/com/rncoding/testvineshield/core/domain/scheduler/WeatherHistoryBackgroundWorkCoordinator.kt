package com.rncoding.testvineshield.core.domain.scheduler

class WeatherHistoryBackgroundWorkCoordinator(
    private val vineyardSource: BackgroundWorkVineyardSource,
    private val scheduler: WeatherHistoryRiskScheduler
) {

    suspend fun scheduleForUser(userId: Long) {
        require(userId > 0L) {
            "User ID must be positive."
        }

        val vineyardIds = vineyardSource.getVineyardIds(userId)

        vineyardIds.forEach { vineyardId ->
            scheduler.schedule(
                userId = userId,
                vineyardId = vineyardId
            )
        }
    }
}