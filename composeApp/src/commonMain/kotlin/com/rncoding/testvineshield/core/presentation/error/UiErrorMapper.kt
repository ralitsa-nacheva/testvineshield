package com.rncoding.testvineshield.core.presentation.error

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.NetworkError
import com.rncoding.testvineshield.core.domain.error.SecurityError
import com.rncoding.testvineshield.core.domain.error.StorageError
import com.rncoding.testvineshield.core.domain.error.ValidationError

class UiErrorMapper {

    fun map(
        error: AppError
    ): UiError {

        return when (error) {

            // -------------------------------------------------
            // AUTHENTICATION
            // -------------------------------------------------

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
                    message = error.userMessage,
                    confirmText = "Log in"
                )

            AuthError.SessionExpired ->
                UiError.Dialog(
                    title = "Session expired",
                    message = error.userMessage,
                    confirmText = "Log in"
                )

            AuthError.NoActiveSession ->
                UiError.Snackbar(
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

            // -------------------------------------------------
            // VALIDATION
            // -------------------------------------------------

            ValidationError.InvalidEmail ->
                UiError.Inline(
                    field = UiError.Field.EMAIL,
                    message = error.userMessage
                )

            is ValidationError.PasswordTooShort,
            ValidationError.PasswordMissingUppercase,
            ValidationError.PasswordMissingDigit ->
                UiError.Inline(
                    field = UiError.Field.PASSWORD,
                    message = error.userMessage
                )

            is ValidationError.FieldEmpty ->
                UiError.Inline(
                    field = error.field.toUiField(),
                    message = error.userMessage
                )

            is ValidationError.TooLong ->
                UiError.Inline(
                    field = error.field.toUiField(),
                    message = error.userMessage
                )

            is ValidationError.InvalidFormat ->
                UiError.Inline(
                    field = error.field.toUiField(),
                    message = error.userMessage
                )

            // -------------------------------------------------
            // NETWORK
            // -------------------------------------------------

            NetworkError.NoInternet ->
                UiError.Snackbar(
                    message = error.userMessage,
                    action = UiError.Action.RETRY
                )

            NetworkError.Timeout ->
                UiError.Snackbar(
                    message = error.userMessage,
                    action = UiError.Action.RETRY
                )

            is NetworkError.HttpError ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            is NetworkError.SerializationError ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            is NetworkError.Unknown ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            // -------------------------------------------------
            // DATABASE
            // -------------------------------------------------

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
                UiError.Dialog(
                    title = "Data error",
                    message = error.userMessage
                )

            // -------------------------------------------------
            // SECURE STORAGE
            // -------------------------------------------------

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

            // -------------------------------------------------
            // SECURITY / BIOMETRICS
            // -------------------------------------------------

            SecurityError.BiometricNotAvailable ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            SecurityError.BiometricNotEnrolled ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            SecurityError.AuthenticationFailed ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            SecurityError.AuthenticationCancelled ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            SecurityError.AuthenticationLockedOut ->
                UiError.Dialog(
                    title = "Biometric authentication unavailable",
                    message = error.userMessage
                )

            SecurityError.AuthenticationPermanentlyLockedOut ->
                UiError.Dialog(
                    title = "Biometric authentication unavailable",
                    message = error.userMessage
                )

            SecurityError.SecurityUpdateRequired ->
                UiError.Dialog(
                    title = "Security update required",
                    message = error.userMessage
                )

            SecurityError.NoDeviceCredential ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            SecurityError.PasscodeNotSet ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            is SecurityError.Unknown ->
                UiError.Snackbar(
                    message = error.userMessage
                )

            // -------------------------------------------------
            // FALLBACK
            // -------------------------------------------------

            else ->
                UiError.Snackbar(
                    message = error.userMessage
                )
        }
    }

    private fun ValidationError.Field.toUiField(): UiError.Field =
        when (this) {
            ValidationError.Field.EMAIL ->
                UiError.Field.EMAIL

            ValidationError.Field.PASSWORD ->
                UiError.Field.PASSWORD

            ValidationError.Field.NAME ->
                UiError.Field.NAME

            ValidationError.Field.VINEYARD_NAME ->
                UiError.Field.VINEYARD_NAME
        }
}

