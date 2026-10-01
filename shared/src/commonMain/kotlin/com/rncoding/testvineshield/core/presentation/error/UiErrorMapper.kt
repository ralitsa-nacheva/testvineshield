package com.rncoding.testvineshield.core.presentation.error

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.NetworkError
import com.rncoding.testvineshield.core.domain.error.SecurityError
import com.rncoding.testvineshield.core.domain.error.StorageError
import com.rncoding.testvineshield.core.domain.error.ValidationError
import com.rncoding.testvineshield.core.domain.error.VineyardField
import com.rncoding.testvineshield.core.domain.error.VineyardValidationError

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
            SecurityError.DeviceCredentialNotAvailable,
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
                    field = error.field.toUiField(),
                    message = error.userMessage
                )

            is ValidationError.TooShort ->
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

            ValidationError.PasswordMissingUppercase ->
                UiError.Inline(
                    field = UiError.Field.PASSWORD,
                    message = error.userMessage
                )

            ValidationError.PasswordMissingDigit ->
                UiError.Inline(
                    field = UiError.Field.PASSWORD,
                    message = error.userMessage
                )
            is VineyardValidationError ->
                UiError.Inline(
                    field = error.field.toUiField(),
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

    private fun ValidationError.Field.toUiField():
            UiError.Field {

        return when (this) {

            ValidationError.Field.EMAIL ->
                UiError.Field.EMAIL

            ValidationError.Field.PASSWORD ->
                UiError.Field.PASSWORD

            ValidationError.Field.NAME ->
                UiError.Field.NAME
        }
    }

    private fun VineyardField.toUiField(): UiError.Field {
        return when (this) {
            VineyardField.NAME ->
                UiError.Field.VINEYARD_NAME

            VineyardField.SIZE ->
                UiError.Field.VINEYARD_SIZE

            VineyardField.COUNTRY ->
                UiError.Field.COUNTRY

            VineyardField.CITY ->
                UiError.Field.CITY

            VineyardField.LATITUDE ->
                UiError.Field.LATITUDE

            VineyardField.LONGITUDE ->
                UiError.Field.LONGITUDE

            VineyardField.TIME_ZONE ->
                UiError.Field.TIME_ZONE

            VineyardField.ELEVATION ->
                UiError.Field.ELEVATION
        }
    }
}