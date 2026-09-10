package com.rncoding.testvineshield.core.presentation.error

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.NetworkError
import com.rncoding.testvineshield.core.domain.error.SecurityError
import com.rncoding.testvineshield.core.domain.error.StorageError
import com.rncoding.testvineshield.core.domain.error.ValidationError

/**
 * Converts domain errors into presentation-level UI errors.
 *
 * The UI should not need to understand the complete domain
 * error hierarchy.
 */
class UiErrorMapper {

    fun map(error: AppError): UiError {

        return when (error) {

            // -------------------------
            // AUTH
            // -------------------------

            AuthError.InvalidEmail ->
                UiError.Inline(
                    field = "email",
                    message = error.userMessage
                )

            is AuthError.WeakPassword ->
                UiError.Inline(
                    field = "password",
                    message = error.userMessage
                )

            AuthError.InvalidCredentials ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            AuthError.UserAlreadyExists ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            AuthError.Unauthorized ->
                UiError.Dialog(
                    title = "Authentication required",
                    message = error.userMessage
                )

            AuthError.NoActiveSession ->
                UiError.Dialog(
                    title = "Login required",
                    message = error.userMessage
                )

            AuthError.SessionExpired ->
                UiError.Dialog(
                    title = "Session expired",
                    message = error.userMessage
                )

            AuthError.UserNotFound ->
                UiError.Dialog(
                    title = "Account unavailable",
                    message = error.userMessage
                )

            AuthError.UnlockFailed ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            // -------------------------
            // NETWORK
            // -------------------------

            NetworkError.NoInternet ->
                UiError.Snackbar(
                    message = error.userMessage,
                    action = "Retry"
                )

            NetworkError.Timeout ->
                UiError.Snackbar(
                    message = error.userMessage,
                    action = "Retry"
                )

            is NetworkError.HttpError ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            is NetworkError.SerializationError ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            is NetworkError.UnknownNetworkError ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            // -------------------------
            // DATABASE
            // -------------------------

            is DatabaseError.NotFound ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            is DatabaseError.ConstraintViolation ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            is DatabaseError.TransactionFailed ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            is DatabaseError.Unknown ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            // -------------------------
            // STORAGE
            // -------------------------

            is StorageError.ReadError ->
                UiError.Dialog(
                    title = "Storage error",
                    message = error.userMessage
                )

            is StorageError.WriteError ->
                UiError.Dialog(
                    title = "Storage error",
                    message = error.userMessage
                )

            is StorageError.DeleteError ->
                UiError.Dialog(
                    title = "Storage error",
                    message = error.userMessage
                )

            is StorageError.CorruptedData ->
                UiError.Dialog(
                    title = "Saved data unavailable",
                    message = error.userMessage
                )

            // -------------------------
            // SECURITY
            // -------------------------

            SecurityError.BiometricNotAvailable,
            SecurityError.BiometricNotEnrolled,
            SecurityError.AuthenticationFailed,
            SecurityError.AuthenticationCancelled,
            SecurityError.AuthenticationLockedOut,
            SecurityError.AuthenticationPermanentlyLockedOut,
            SecurityError.SecurityUpdateRequired,
            SecurityError.NoDeviceCredential,
            SecurityError.PasscodeNotSet,
            SecurityError.UserFallback ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            is SecurityError.Unknown ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            // -------------------------
            // VALIDATION
            // -------------------------

            is ValidationError.FieldEmpty ->
                UiError.Inline(
                    field = error.field,
                    message = error.userMessage
                )

            is ValidationError.TooShort ->
                UiError.Inline(
                    field = error.field,
                    message = error.userMessage
                )

            is ValidationError.TooLong ->
                UiError.Inline(
                    field = error.field,
                    message = error.userMessage
                )

            is ValidationError.InvalidFormat ->
                UiError.Inline(
                    field = error.field,
                    message = error.userMessage
                )

            // -------------------------
            // UNKNOWN / FUTURE ERRORS
            // -------------------------

            else ->
                UiError.Snackbar(
                    message = error.userMessage
                )
        }
    }
}