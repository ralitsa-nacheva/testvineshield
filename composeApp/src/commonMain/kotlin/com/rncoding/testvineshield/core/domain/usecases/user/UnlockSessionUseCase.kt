package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.repository.UserRepository

class UnlockSessionUseCase(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val localAuthenticator: LocalAuthenticator
) {

    suspend operator fun invoke():
            Result<UserDomainModel, AppError> {

        return when (
            val authentication =
                localAuthenticator.authenticate()
        ) {

            is Result.Error ->
                Result.Error(
                    authentication.error
                )

            is Result.Success -> {

                when (
                    val session =
                        sessionManager.unlock()
                ) {

                    is Result.Error ->
                        Result.Error(
                            session.error
                        )

                    is Result.Success -> {

                        when (
                            val user =
                                userRepository.getUserById(
                                    session.data.userId
                                )
                        ) {

                            is Result.Success ->
                                Result.Success(
                                    user.data
                                )

                            is Result.Error ->
                                Result.Error(
                                    user.error
                                )
                        }
                    }
                }
            }
        }
    }
}