package com.rncoding.testvineshield.core.domain.error

data class UnexpectedError(
    override val cause: Throwable? = null
) : AppError {

    override val userMessage =
        "Something unexpected happened."

    override val debugMessage =
        cause?.message ?: "Unexpected application error."
}