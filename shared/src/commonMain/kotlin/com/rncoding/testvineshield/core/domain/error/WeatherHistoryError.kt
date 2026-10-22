
package com.rncoding.testvineshield.core.domain.error

sealed interface WeatherHistoryError : AppError {

    data class InvalidInput(
        override val debugMessage: String
    ) : WeatherHistoryError {
        override val userMessage =
            "The vineyard location is invalid."

        override val cause: Throwable? = null
    }

    data class TimeZoneMismatch(
        val expected: String,
        val actual: String
    ) : WeatherHistoryError {
        override val userMessage =
            "The weather service returned inconsistent location data."

        override val debugMessage =
            "Expected timezone '$expected', received '$actual'."

        override val cause: Throwable? = null
    }

    data class EvaluationFailed(
        override val cause: Throwable
    ) : WeatherHistoryError {
        override val userMessage =
            "Weather was updated, but disease-risk evaluation failed."

        override val debugMessage =
            cause.message
    }
}
