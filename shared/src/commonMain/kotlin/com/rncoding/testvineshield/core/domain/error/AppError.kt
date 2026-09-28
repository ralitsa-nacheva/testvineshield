package com.rncoding.testvineshield.core.domain.error

sealed interface AppError {
    val userMessage: String
    val debugMessage: String?
    val cause: Throwable?
}