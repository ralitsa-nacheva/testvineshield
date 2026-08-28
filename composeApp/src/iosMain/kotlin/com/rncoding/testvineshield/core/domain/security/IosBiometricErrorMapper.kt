package com.rncoding.testvineshield.core.domain.security

import com.rncoding.testvineshield.core.domain.error.SecurityError
import platform.LocalAuthentication.kLAErrorAppCancel
import platform.LocalAuthentication.kLAErrorAuthenticationFailed
import platform.LocalAuthentication.kLAErrorBiometryLockout
import platform.LocalAuthentication.kLAErrorBiometryNotAvailable
import platform.LocalAuthentication.kLAErrorBiometryNotEnrolled
import platform.LocalAuthentication.kLAErrorInvalidContext
import platform.LocalAuthentication.kLAErrorPasscodeNotSet
import platform.LocalAuthentication.kLAErrorSystemCancel
import platform.LocalAuthentication.kLAErrorUserCancel
import platform.LocalAuthentication.kLAErrorUserFallback

internal object IosBiometricErrorMapper {

    fun map(
        errorCode: Long
    ): SecurityError {

        return when (errorCode.toInt()) {

            kLAErrorAuthenticationFailed ->
                SecurityError.AuthenticationFailed

            kLAErrorUserCancel,
            kLAErrorSystemCancel,
            kLAErrorAppCancel ->
                SecurityError.AuthenticationCancelled

            kLAErrorBiometryNotAvailable ->
                SecurityError.BiometricNotAvailable

            kLAErrorBiometryNotEnrolled ->
                SecurityError.BiometricNotEnrolled

            kLAErrorBiometryLockout ->
                SecurityError.AuthenticationLockedOut

            kLAErrorPasscodeNotSet ->
                SecurityError.PasscodeNotSet

            kLAErrorUserFallback ->
                SecurityError.UserFallback

            kLAErrorInvalidContext ->
                SecurityError.Unknown()

            else ->
                SecurityError.Unknown()
        }
    }
}