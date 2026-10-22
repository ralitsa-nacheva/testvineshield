
package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.error.NetworkError
import com.rncoding.testvineshield.core.domain.error.WeatherHistoryError
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeatherHistoryRetryPolicyTest {

    @Test
    fun noInternetShouldRetry() {
        assertTrue(
            WeatherHistoryRetryPolicy.shouldRetry(
                NetworkError.NoInternet
            )
        )
    }

    @Test
    fun timeoutShouldRetry() {
        assertTrue(
            WeatherHistoryRetryPolicy.shouldRetry(
                NetworkError.Timeout
            )
        )
    }

    @Test
    fun transientHttpErrorsShouldRetry() {
        listOf(408, 429, 500, 502, 503, 504, 599)
            .forEach { statusCode ->
                val error = NetworkError.HttpError(
                    code = statusCode,
                    debugMessage = "HTTP $statusCode",
                    cause = null
                )

                assertTrue(
                    WeatherHistoryRetryPolicy.shouldRetry(error),
                    "Expected retry for HTTP $statusCode"
                )
            }
    }

    @Test
    fun permanentHttpErrorsShouldNotRetry() {
        listOf(400, 401, 403, 404, 422)
            .forEach { statusCode ->
                val error = NetworkError.HttpError(
                    code = statusCode,
                    debugMessage = "HTTP $statusCode",
                    cause = null
                )

                assertFalse(
                    WeatherHistoryRetryPolicy.shouldRetry(error),
                    "Did not expect retry for HTTP $statusCode"
                )
            }
    }

    @Test
    fun invalidInputShouldNotRetry() {
        assertFalse(
            WeatherHistoryRetryPolicy.shouldRetry(
                WeatherHistoryError.InvalidInput(
                    debugMessage = "Invalid coordinates"
                )
            )
        )
    }

    @Test
    fun timeZoneMismatchShouldNotRetry() {
        assertFalse(
            WeatherHistoryRetryPolicy.shouldRetry(
                WeatherHistoryError.TimeZoneMismatch(
                    expected = "Europe/Sofia",
                    actual = "UTC"
                )
            )
        )
    }

    @Test
    fun evaluationFailureShouldNotRetry() {
        assertFalse(
            WeatherHistoryRetryPolicy.shouldRetry(
                WeatherHistoryError.EvaluationFailed(
                    cause = IllegalStateException(
                        "Risk evaluation failed"
                    )
                )
            )
        )
    }
}
