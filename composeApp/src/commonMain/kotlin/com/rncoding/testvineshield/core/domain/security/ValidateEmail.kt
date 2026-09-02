package com.rncoding.testvineshield.core.domain.security

import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.ValidationError
class ValidateEmail {

    operator fun invoke(
        email: String
    ): Result<Unit, ValidationError> {

        val normalized = email.trim()

        if (normalized.isEmpty()) {
            return Result.Error(
                ValidationError.FieldEmpty(
                    ValidationError.Field.EMAIL
                )
            )
        }

        if (!EMAIL_REGEX.matches(normalized)) {
            return Result.Error(
                ValidationError.InvalidEmail
            )
        }

        return Result.Success(Unit)
    }

    private companion object {

        /**
         * Deliberately conservative application-level email validation.
         *
         * Email validation should not attempt to implement the
         * complete RFC specification.
         */
        val EMAIL_REGEX =
            Regex(
                pattern = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
            )
    }
}