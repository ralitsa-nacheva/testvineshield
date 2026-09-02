package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.datamodels.SessionDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.time.SystemClock
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import com.rncoding.testvineshield.core.domain.security.SessionPolicy
import com.rncoding.testvineshield.core.domain.security.ValidateEmail
import com.rncoding.testvineshield.core.domain.security.ValidatePassword

class LoginUserUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val validateEmail: ValidateEmail,
    private val validatePassword: ValidatePassword,
    private val clock: SystemClock
) {

    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<UserDomainModel, AppError> {

        when (val validation = validateEmail(email)) {

            is Result.Error ->
                return Result.Error(validation.error)

            is Result.Success -> Unit
        }

        when (val validation = validatePassword(password)) {

            is Result.Error ->
                return Result.Error(validation.error)

            is Result.Success -> Unit
        }

        return when (
            val loginResult =
                userRepository.login(
                    email = email.trim(),
                    password = password
                )
        ) {

            is Result.Error ->
                Result.Error(loginResult.error)

            is Result.Success -> {

                val now = clock.now()

                val session =
                    SessionDomainModel(
                        userId = loginResult.data.userId,
                        createdAt = now,
                        lastActiveAt = now,
                        expiresAt =
                            now +
                                    SessionPolicy.MAX_SESSION_DURATION
                    )

                when (
                    val saveResult =
                        sessionRepository.saveSession(session)
                ) {

                    is Result.Error ->
                        Result.Error(saveResult.error)

                    is Result.Success ->
                        Result.Success(loginResult.data)
                }
            }
        }
    }
}