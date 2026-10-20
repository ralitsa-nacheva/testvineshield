
package com.rncoding.testvineshield.core.domain.datamodels

data class WeatherHistoryRefreshResult(
    val vineyardId: Long,
    val fetchedAt: Long,
    val receivedCompletedHours: Int,
    val insertedHours: Int
) {
    init {
        require(vineyardId > 0L)
        require(fetchedAt > 0L)
        require(receivedCompletedHours >= 0)
        require(insertedHours in 0..receivedCompletedHours)
    }

    val ignoredHours: Int
        get() = receivedCompletedHours - insertedHours

    val hasNewWeather: Boolean
        get() = insertedHours > 0
}
