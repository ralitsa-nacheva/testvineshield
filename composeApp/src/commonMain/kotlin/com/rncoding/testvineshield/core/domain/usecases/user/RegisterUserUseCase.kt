package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.ValidationError
import com.rncoding.testvineshield.core.domain.security.ValidateEmail
import com.rncoding.testvineshield.core.domain.security.ValidatePassword

class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val validateEmail: ValidateEmail,
    private val validatePassword: ValidatePassword
) {

    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<UserDomainModel, AppError> {

        when (val result = validateEmail(email)) {

            is Result.Error ->
                return Result.Error(result.error)

            is Result.Success ->
                Unit
        }

        when (val result = validatePassword(password)) {

            is Result.Error ->
                return Result.Error(result.error)

            is Result.Success ->
                Unit
        }

        return userRepository.register(
            email = email.trim(),
            password = password
        )
    }
}