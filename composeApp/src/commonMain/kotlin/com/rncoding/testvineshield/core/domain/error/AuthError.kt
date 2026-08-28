package com.rncoding.testvineshield.core.domain.error

sealed interface AuthError : AppError {
    data object InvalidEmail : AuthError {
        override val userMessage = "Invalid email format"
        override val debugMessage = "Email validation failed" //implement the validation rules
        override val cause: Throwable? = null
    }

    data class WeakPassword (val reason: Reason) : AuthError {
        enum class Reason {
            TOO_SHORT,
            NO_UPPERCASE,
            NO_DIGIT
        }
        override val userMessage = when (reason) {
            Reason.TOO_SHORT -> "Password must be at least 8 characters."
            Reason.NO_UPPERCASE -> "Password must contain uppercase letter"
            Reason.NO_DIGIT -> "Password must contain a digit"
        }
        override val debugMessage = "Password does not meet criteria"
        override val cause: Throwable? = null
    }

    data object UserAlreadyExists : AuthError {
        override val userMessage = "User already exists"
        override val debugMessage = "Attempt to register existing email"
        override val cause: Throwable? = null
    }

    data object InvalidCredentials : AuthError {
        override val userMessage = "Invalid email or password"
        override val debugMessage = "Login failed due to mismatch"
        override val cause: Throwable? = null
    }

    data object Unauthorized : AuthError {
        override val userMessage = "Session expired. Please login again."
        override val debugMessage = "401 Unauthorized"
        override val cause: Throwable? = null
    }

    data object NoActiveSession : AuthError {
        override val userMessage =
            "Please log in."

        override val debugMessage =
            "No persisted session was found."

        override val cause: Throwable? = null
    }

    data object SessionExpired : AuthError {
        override val userMessage =
            "Your session has expired. Please log in again."

        override val debugMessage =
            "Session exceeded maximum lifetime."

        override val cause: Throwable? = null
    }

    data object UserNotFound : AuthError {
        override val userMessage =
            "Your local account could not be found."

        override val debugMessage =
            "Persisted session referenced a missing user."

        override val cause: Throwable? = null
    }

    data object UnlockFailed : AuthError {
        override val userMessage =
            "Unable to unlock the application."

        override val debugMessage =
            "Local authentication failed."

        override val cause: Throwable? = null
    }
}