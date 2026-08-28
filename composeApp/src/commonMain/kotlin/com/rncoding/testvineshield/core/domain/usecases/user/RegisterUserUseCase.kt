package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.ValidationError
import com.rncoding.testvineshield.core.domain.security.ValidateEmail
import com.rncoding.testvineshield.core.domain.security.ValidatePassword

class RegisterUserUseCase( // check to see if i need to implement more validation rules here or in the view model/screen
    private val userRepository: UserRepository,
    private val validateEmail: ValidateEmail,
    private val validatePassword: ValidatePassword
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<UserDomainModel, AppError> {

        if (email.isBlank()) {
            return Result.Error(ValidationError.FieldEmpty(email)) // implement validation rules
        }
        if (password.isBlank()) {
            return Result.Error(ValidationError.FieldEmpty(password)) // implement validation rules
        }
        if (!validateEmail(email)) {
            return Result.Error(AuthError.InvalidEmail)
        }
        if (!validatePassword(password)) {
            return Result.Error(AuthError.WeakPassword(AuthError.WeakPassword.Reason.TOO_SHORT))
        }

        return userRepository.register(email, password) // should there be auto login after register with saveSession?
    }
}