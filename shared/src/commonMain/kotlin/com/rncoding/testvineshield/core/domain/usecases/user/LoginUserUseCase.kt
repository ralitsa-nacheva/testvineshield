package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.security.SessionManager
import com.rncoding.testvineshield.core.domain.security.ValidateEmail
import com.rncoding.testvineshield.core.domain.security.ValidatePassword


class LoginUserUseCase(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val validateEmail: ValidateEmail,
    private val validatePassword: ValidatePassword
) {

    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<UserDomainModel, AppError> {

        when (val validation = validateEmail(email)) {

            is Result.Error ->
                return Result.Error(validation.error)

            is Result.Success ->
                Unit
        }

        when (val validation = validatePassword(password)) {

            is Result.Error ->
                return Result.Error(validation.error)

            is Result.Success ->
                Unit
        }

        val user =
            when (
                val loginResult =
                    userRepository.login(
                        email = email.trim(),
                        password = password
                    )
            ) {

                is Result.Error ->
                    return Result.Error(loginResult.error)

                is Result.Success ->
                    loginResult.data
            }

        when (
            val sessionResult =
                sessionManager.createSession(user.userId)
        ) {

            is Result.Error ->
                return Result.Error(sessionResult.error)

            is Result.Success ->
                Unit
        }

        return Result.Success(user)
    }
}