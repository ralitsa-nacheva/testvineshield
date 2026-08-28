package com.rncoding.testvineshield.core.presentation.error

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.NetworkError
import com.rncoding.testvineshield.core.domain.error.ValidationError

class UiErrorMapper {
    fun map(error: AppError): UiError {
        return when (error) {
            // 🔐 AUTH
            is AuthError.InvalidCredentials ->
                UiError.Snackbar("Invalid email or password")

            is AuthError.UserAlreadyExists ->
                UiError.Snackbar("User already exists")

            is AuthError.SessionExpired ->
                UiError.Dialog(
                    title = "Session expired",
                    message = "Please log in again"
                )
// 🌐 NETWORK
            is NetworkError.NoInternet ->
                UiError.Snackbar(
                    message = "No internet connection",
                    action = "Retry"
                )

            is NetworkError.Timeout ->
                UiError.Snackbar("Request timed out")
// 🗄 DATABASE
            is DatabaseError.Corrupted ->
                UiError.Dialog(
                    title = "Data error",
                    message = "Database corrupted. Restart app."
                )
// 🧪 VALIDATION
            is ValidationError.InvalidEmail ->
                UiError.Inline(
                    field = "email",
                    message = "Invalid email"
                )

            is ValidationError.WeakPassword ->
                UiError.Inline(
                    field = "password",
                    message = error.message ?: "Weak password"
                )
// ❓ DEFAULT
            else ->
                UiError.Snackbar(error.message ?: "Unknown error")
        }
    }
}

