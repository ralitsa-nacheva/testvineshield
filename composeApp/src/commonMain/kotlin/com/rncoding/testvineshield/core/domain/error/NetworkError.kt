package com.rncoding.testvineshield.core.domain.error

sealed interface NetworkError : AppError {
    data object NoInternet : NetworkError {
        override val userMessage = "No internet connection"
        override val debugMessage = "Network unavailable"
        override val cause: Throwable? = null
    }
    data object Timeout : NetworkError {
        override val userMessage = "Request timed out"
        override val debugMessage = "Socket timeout"
        override val cause: Throwable? = null
    }
    data class HttpError(
        val code: Int,
        override val debugMessage: String?,
        override val cause: Throwable? = null
    ) : NetworkError {
        override val userMessage = when (code) {
            400 -> "Bad request"
            401 -> "Unauthorized"
            404 -> "Not found"
            500 -> "Server error"
            else -> "Network error"
        }
    }
    data class SerializationError(
        override val cause: Throwable? = null
    ) : NetworkError {
        override val userMessage = "Data parsing error"
        override val debugMessage = cause?.message
    }
    data class UnknownNetworkError(
        override val cause: Throwable? = null
    ) : NetworkError {
        override val userMessage = "Unexpected network error"
        override val debugMessage = cause?.message
    }
}