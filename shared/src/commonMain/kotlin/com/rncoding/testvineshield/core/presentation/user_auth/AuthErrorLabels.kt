package com.rncoding.testvineshield.core.presentation.user_auth

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

/** Exhaustive per-family mappings preserve domain identity and keep diagnostics out of UI. */
@Composable
internal fun localizedAuthErrorMessage(error: AppError): String = when (error) {
    is AuthError -> stringResource(when (error) {
        AuthError.UserAlreadyExists -> Res.string.auth_error_exists
        AuthError.InvalidCredentials -> Res.string.auth_error_credentials
        AuthError.Unauthorized -> Res.string.auth_error_unauthorized
        AuthError.NoActiveSession -> Res.string.auth_error_login
        AuthError.SessionExpired -> Res.string.auth_error_expired
        AuthError.UserNotFound -> Res.string.auth_error_missing
        AuthError.UnlockFailed -> Res.string.auth_error_unlock
    })
    is SecurityError -> stringResource(when (error) {
        SecurityError.InvalidLockSettings -> Res.string.auth_security_period
        SecurityError.AuthenticationInProgress -> Res.string.auth_security_busy
        SecurityError.BiometricNotAvailable -> Res.string.auth_security_unavailable
        SecurityError.BiometricNotEnrolled -> Res.string.auth_security_enroll
        SecurityError.AuthenticationFailed -> Res.string.auth_security_failed
        SecurityError.AuthenticationCancelled -> Res.string.auth_security_cancelled
        SecurityError.AuthenticationLockedOut -> Res.string.auth_security_locked
        SecurityError.AuthenticationPermanentlyLockedOut -> Res.string.auth_security_device_locked
        SecurityError.SecurityUpdateRequired -> Res.string.auth_security_update
        SecurityError.DeviceCredentialNotAvailable -> Res.string.auth_security_credential
        SecurityError.PasscodeNotSet -> Res.string.auth_security_passcode
        SecurityError.UserFallback -> Res.string.auth_security_fallback
        is SecurityError.Unknown -> Res.string.auth_security_unknown
    })
    is ValidationError -> when (error) {
        is ValidationError.FieldEmpty -> stringResource(Res.string.auth_validation_required, localizedAuthField(error.field))
        is ValidationError.TooShort -> stringResource(Res.string.auth_validation_short, localizedAuthField(error.field))
        is ValidationError.TooLong -> stringResource(Res.string.auth_validation_long, localizedAuthField(error.field))
        is ValidationError.InvalidFormat -> stringResource(Res.string.auth_validation_format, localizedAuthField(error.field))
        ValidationError.PasswordMissingUppercase -> stringResource(Res.string.auth_validation_uppercase)
        ValidationError.PasswordMissingDigit -> stringResource(Res.string.auth_validation_digit)
    }
    else -> stringResource(Res.string.error_action_failed)
}

@Composable
private fun localizedAuthField(field: ValidationError.Field): String = stringResource(when (field) {
    ValidationError.Field.NAME -> Res.string.auth_field_name
    ValidationError.Field.EMAIL -> Res.string.auth_field_email
    ValidationError.Field.PASSWORD -> Res.string.auth_field_password
})
