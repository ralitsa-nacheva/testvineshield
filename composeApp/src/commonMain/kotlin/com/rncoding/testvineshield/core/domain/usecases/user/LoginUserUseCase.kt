package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.datamodels.SessionDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
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
    ): Result<UserDomainModel, AuthError> {

        if (!validateEmail(email)) {
            return Result.Error(AuthError.InvalidEmail)
        }

        if (!validatePassword(password)) {
            return Result.Error(
                AuthError.WeakPassword(
                    AuthError.WeakPassword.Reason.TOO_SHORT // implement the other validation rules
                )
            )
        }

        return when (
            val result = userRepository.login(
                email = email,
                password = password
            )
        ) {

            is Result.Error -> result

            is Result.Success -> {

                val now = clock.now()

                val session = SessionDomainModel(
                    userId = result.data.userId,
                    createdAt = now,
                    lastActiveAt = now,
                    expiresAt =
                        now + SessionPolicy.MAX_SESSION_DURATION
                )

                when (
                    val saveResult =
                        sessionRepository.saveSession(session)
                ) {

                    is Result.Success ->
                        Result.Success(result.data)

                    is Result.Error ->
                        Result.Error(
                            AuthError.UnlockFailed
                        )
                }
            }
        }
    }
}
