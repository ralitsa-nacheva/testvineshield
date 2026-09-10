package com.rncoding.testvineshield.core.domain.error

import com.rncoding.testvineshield.core.domain.error.AppError

sealed interface ValidationError : AppError {

    enum class Field(
        val displayName: String
    ) {
        NAME("Name"),
        EMAIL("Email"),
        PASSWORD("Password")
    }

    data class FieldEmpty(
        val field: Field
    ) : ValidationError {
        override val userMessage =
            "${field.displayName} is required."
        override val debugMessage =
            "Required field is empty: $field"
        override val cause: Throwable? = null
    }

    data class TooShort(
        val field: Field,
        val min: Int
    ) : ValidationError {
        override val userMessage =
            "${field.displayName} is too short."
        override val debugMessage =
            "$field is shorter than minimum length $min"
        override val cause: Throwable? = null
    }

    data class TooLong(
        val field: Field,
        val max: Int
    ) : ValidationError {
        override val userMessage =
            "${field.displayName} is too long."
        override val debugMessage =
            "$field exceeds maximum length $max"
        override val cause: Throwable? = null
    }

    data class InvalidFormat(
        val field: Field
    ) : ValidationError {
        override val userMessage =
            "${field.displayName} has an invalid format."
        override val debugMessage =
            "Invalid format for field: $field"
        override val cause: Throwable? = null
    }

    data object PasswordMissingUppercase : ValidationError {
        override val userMessage =
            "Password must contain at least one uppercase letter."
        override val debugMessage =
            "Password contains no uppercase character."
        override val cause: Throwable? = null
    }

    data object PasswordMissingDigit : ValidationError {
        override val userMessage =
            "Password must contain at least one digit."
        override val debugMessage =
            "Password contains no digit."
        override val cause: Throwable? = null
    }
}