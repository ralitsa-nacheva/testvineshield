package com.rncoding.testvineshield.core.domain.security

import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.ValidationError

class ValidatePassword {

    operator fun invoke(
        password: String
    ): Result<Unit, ValidationError> {

        if (password.isEmpty()) {
            return Result.Error(
                ValidationError.FieldEmpty(
                    ValidationError.Field.PASSWORD
                )
            )
        }

        if (password.length < MIN_LENGTH) {
            return Result.Error(
                ValidationError.PasswordTooShort(
                    minimumLength = MIN_LENGTH
                )
            )
        }

        if (!password.any(Char::isUpperCase)) {
            return Result.Error(
                ValidationError.PasswordMissingUppercase
            )
        }

        if (!password.any(Char::isDigit)) {
            return Result.Error(
                ValidationError.PasswordMissingDigit
            )
        }

        return Result.Success(Unit)
    }

    private companion object {
        const val MIN_LENGTH = 8
    }
}