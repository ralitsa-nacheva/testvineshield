package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.*
import com.rncoding.testvineshield.core.domain.error.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

/** Best-effort bounded bookkeeping; never hides cancellation/the original failure. */
internal suspend fun recordWeatherFailure(dao: WeatherCacheDao,request: WeatherCacheRequest,error: AppError?) {
    val code=when(error) {
        NetworkError.NoInternet -> "network_unavailable"
        NetworkError.Timeout -> "timeout"
        is NetworkError.HttpError -> "http_${error.code}"
        is NetworkError.SerializationError -> "invalid_response"
        is WeatherCacheSourceChanged -> "source_changed"
        null -> "interrupted"
        else -> "refresh_failed"
    }
    withContext(NonCancellable) {
        try { withTimeout(2.seconds) { dao.fail(request,code) } } catch (_: Exception) { /* unfinished status stays explicit */ }
    }
}
