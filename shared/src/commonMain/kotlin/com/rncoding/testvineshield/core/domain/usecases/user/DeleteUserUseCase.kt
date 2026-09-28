package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.security.SessionManager

class DeleteUserUseCase(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) {

    suspend operator fun invoke(
        userId: Long,
        currentPassword: String
    ): Result<Unit, AppError> {

        val result = userRepository.deleteUser(
            userId = userId,
            currentPassword = currentPassword
        )

        return when (result) {

            is Result.Error ->
                Result.Error(result.error)

            is Result.Success -> {

                when (val clearResult = sessionManager.clearSession()) {

                    is Result.Success ->
                        Result.Success(Unit)

                    is Result.Error ->
                        Result.Error(clearResult.error)
                }
            }
        }
    }
}