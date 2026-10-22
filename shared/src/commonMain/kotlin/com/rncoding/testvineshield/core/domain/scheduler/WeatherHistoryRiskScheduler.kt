
package com.rncoding.testvineshield.core.domain.scheduler

interface WeatherHistoryRiskScheduler {

    fun schedule(
        userId: Long,
        vineyardId: Long
    )

    fun cancel(
        userId: Long,
        vineyardId: Long
    )

    fun cancelAllForUser(
        userId: Long
    )
}
