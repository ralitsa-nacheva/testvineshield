package com.rncoding.testvineshield.core.domain.error

/** UI-safe startup failure. The underlying database exception is not displayed. */
data object AppInitializationError : AppError {
    override val userMessage = "Local application data could not be prepared. Please try again."
    override val debugMessage = "Application initialization failed."
    override val cause: Throwable? = null
}
