package com.rncoding.testvineshield.core.domain.error

sealed interface NetworkError : AppError {

    data object NoInternet : NetworkError {

        override val userMessage =
            "No internet connection."

        override val debugMessage =
            "Network connectivity unavailable."

        override val cause: Throwable? = null
    }

    data object Timeout : NetworkError {

        override val userMessage =
            "The request timed out. Please try again."

        override val debugMessage =
            "Network request timed out."

        override val cause: Throwable? = null
    }

    data class HttpError(
        val code: Int,
        override val debugMessage: String?,
        override val cause: Throwable? = null
    ) : NetworkError {

        override val userMessage: String =
            when (code) {
                400 -> "The request was invalid."
                401 -> "Authentication is required."
                403 -> "You are not authorized."
                404 -> "The requested resource was not found."
                429 -> "Too many requests. Please try again later."
                in 500..599 -> "The server is temporarily unavailable."
                else -> "A network error occurred."
            }
    }

    data class SerializationError(
        override val cause: Throwable? = null
    ) : NetworkError {

        override val userMessage =
            "The server returned invalid data."

        override val debugMessage =
            cause?.message ?: "Network response serialization failed."
    }

    data class Unknown(
        override val cause: Throwable? = null
    ) : NetworkError {

        override val userMessage =
            "An unexpected network error occurred."

        override val debugMessage =
            cause?.message ?: "Unknown network error."
    }
}