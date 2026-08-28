package com.rncoding.testvineshield.core.domain.error

sealed interface ValidationError : AppError {
    data class FieldEmpty(val field: String) : ValidationError {
        override val userMessage = "$field cannot be empty"
        override val debugMessage = "Empty field: $field"
        override val cause: Throwable? = null
    }
    data class TooShort(val field: String, val min: Int) : ValidationError {
        override val userMessage = "$field must be at least $min characters"
        override val debugMessage = "Too short: $field"
        override val cause: Throwable? = null
    }
    data class TooLong(val field: String, val max: Int) : ValidationError {
        override val userMessage = "$field must be less than $max characters"
        override val debugMessage = "Too long: $field"
        override val cause: Throwable? = null
    }
    data class InvalidFormat(val field: String) : ValidationError {
        override val userMessage = "Invalid $field format"
        override val debugMessage = "Invalid format: $field"
        override val cause: Throwable? = null
    }
}