
package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.NetworkError

object WeatherHistoryRetryPolicy {

    fun shouldRetry(error: AppError): Boolean =
        when (error) {

            NetworkError.NoInternet,
            NetworkError.Timeout -> true

            is NetworkError.HttpError ->
                error.code == 408 ||
                        error.code == 429 ||
                        error.code in 500..599

            else -> false
        }
}
