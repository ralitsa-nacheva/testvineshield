package com.rncoding.testvineshield.core.domain.error

sealed interface ValidationError : AppError {

    data class FieldEmpty(
        val field: Field
    ) : ValidationError {

        override val userMessage: String =
            when (field) {
                Field.EMAIL -> "Email cannot be empty."
                Field.PASSWORD -> "Password cannot be empty."
                Field.NAME -> "Name cannot be empty."
                Field.VINEYARD_NAME -> "Vineyard name cannot be empty."
            }

        override val debugMessage: String =
            "Required field was empty: $field"

        override val cause: Throwable? = null
    }

    data object InvalidEmail : ValidationError {

        override val userMessage =
            "Please enter a valid email address."

        override val debugMessage =
            "Email validation failed."

        override val cause: Throwable? = null
    }

    data class PasswordTooShort(
        val minimumLength: Int
    ) : ValidationError {

        override val userMessage =
            "Password must be at least $minimumLength characters."

        override val debugMessage =
            "Password length validation failed."

        override val cause: Throwable? = null
    }

    data object PasswordMissingUppercase : ValidationError {

        override val userMessage =
            "Password must contain an uppercase letter."

        override val debugMessage =
            "Password uppercase-character validation failed."

        override val cause: Throwable? = null
    }

    data object PasswordMissingDigit : ValidationError {

        override val userMessage =
            "Password must contain a digit."

        override val debugMessage =
            "Password digit validation failed."

        override val cause: Throwable? = null
    }

    data class TooLong(
        val field: Field,
        val maximumLength: Int
    ) : ValidationError {

        override val userMessage =
            "${field.displayName} must be less than $maximumLength characters."

        override val debugMessage =
            "Maximum length validation failed for $field."

        override val cause: Throwable? = null
    }

    data class InvalidFormat(
        val field: Field
    ) : ValidationError {

        override val userMessage =
            "Invalid ${field.displayName.lowercase()} format."

        override val debugMessage =
            "Format validation failed for $field."

        override val cause: Throwable? = null
    }

    enum class Field(
        val displayName: String
    ) {
        EMAIL("Email"),
        PASSWORD("Password"),
        NAME("Name"),
        VINEYARD_NAME("Vineyard name")
    }
}