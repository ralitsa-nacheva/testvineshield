package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.validation.ValidateEmail
import com.rncoding.testvineshield.core.domain.validation.ValidatePassword

class UpdateUserUseCase(
    private val userRepository: UserRepository,
    private val validateEmail: ValidateEmail,
    private val validatePassword: ValidatePassword
) {

    suspend operator fun invoke(
        userId: Long,
        currentPassword: String,
        newEmail: String,
        newPassword: String?
    ): Result<UserDomainModel, AppError> {

        val normalizedEmail = newEmail.trim()

        when (val result = validateEmail(normalizedEmail)) {
            is Result.Error ->
                return Result.Error(result.error)

            is Result.Success ->
                Unit
        }

        if (newPassword != null) {
            when (val result = validatePassword(newPassword)) {
                is Result.Error ->
                    return Result.Error(result.error)

                is Result.Success ->
                    Unit
            }
        }

        return userRepository.updateUser(
            userId = userId,
            currentPassword = currentPassword,
            newEmail = normalizedEmail,
            newPassword = newPassword
        )
    }
}