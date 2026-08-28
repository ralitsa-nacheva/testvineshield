package com.rncoding.testvineshield.core.domain.error

data class UnknownError(
    override val cause: Throwable? = null
) : AppError {
    override val userMessage = "Unexpected error occurred"
    override val debugMessage = cause?.message
}
