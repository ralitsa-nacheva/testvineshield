package com.rncoding.testvineshield.core.domain.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.rncoding.testvineshield.core.domain.error.SecurityError

internal object AndroidBiometricErrorMapper {

    fun mapAvailability(
        code: Int
    ): SecurityError? {

        return when (code) {

            BiometricManager.BIOMETRIC_SUCCESS ->
                null

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                SecurityError.BiometricNotAvailable

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                SecurityError.BiometricNotAvailable

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                SecurityError.BiometricNotEnrolled

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                SecurityError.SecurityUpdateRequired

            else ->
                SecurityError.BiometricNotAvailable
        }
    }

    fun mapAuthentication(
        errorCode: Int
    ): SecurityError {

        return when (errorCode) {

            BiometricPrompt.ERROR_CANCELED,
            BiometricPrompt.ERROR_USER_CANCELED ->
                SecurityError.AuthenticationCancelled

            BiometricPrompt.ERROR_HW_UNAVAILABLE ->
                SecurityError.BiometricNotAvailable

            BiometricPrompt.ERROR_LOCKOUT ->
                SecurityError.AuthenticationLockedOut

            BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                SecurityError.AuthenticationPermanentlyLockedOut
            else ->
                SecurityError.Unknown()
        }
    }
}