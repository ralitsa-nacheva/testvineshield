package com.rncoding.testvineshield.core.data.security

import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.SecurityError
import com.rncoding.testvineshield.core.domain.security.LocalAuthenticator
import kotlinx.cinterop.ExperimentalForeignApi
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicy
import platform.LocalAuthentication.LAError
import platform.Foundation.NSError
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosLocalAuthenticator :
    LocalAuthenticator {

    override suspend fun isAvailable():
            Result<Boolean, AppError> {

        val context =
            LAContext()

        var error: NSError? = null

        val available =
            context.canEvaluatePolicy(
                policy =
                    LAPolicy
                        .deviceOwnerAuthenticationWithBiometrics,
                error = error
            )

        if (available) {
            return Result.Success(true)
        }

        return when (
            error?.code?.toInt()
        ) {

            LAError.LAErrorBiometryNotAvailable.toLong()
                .toInt() ->
                Result.Success(false)

            LAError.LAErrorBiometryNotEnrolled.toLong()
                .toInt() ->
                Result.Success(false)

            LAError.LAErrorPasscodeNotSet.toLong()
                .toInt() ->
                Result.Error(
                    SecurityError.PasscodeNotSet
                )

            else ->
                Result.Success(false)
        }
    }

    override suspend fun authenticate(
        reason: String
    ): Result<Unit, AppError> {

        return suspendCancellableCoroutine { continuation ->

            val context =
                LAContext()

            context.evaluatePolicy(
                policy =
                    LAPolicy
                        .deviceOwnerAuthenticationWithBiometrics,

                localizedReason =
                    reason
            ) { success, error ->

                if (
                    !continuation.isActive
                ) {
                    return@evaluatePolicy
                }

                if (success) {

                    continuation.resume(
                        Result.Success(Unit)
                    )

                    return@evaluatePolicy
                }

                continuation.resume(
                    Result.Error(
                        mapAuthenticationError(
                            error
                        )
                    )
                )
            }
        }
    }

    private fun mapAuthenticationError(
        error: NSError?
    ): SecurityError {

        return when (
            error?.code?.toInt()
        ) {

            LAError.LAErrorUserCancel.toLong()
                .toInt() ->
                SecurityError.AuthenticationCancelled

            LAError.LAErrorSystemCancel.toLong()
                .toInt() ->
                SecurityError.AuthenticationCancelled

            LAError.LAErrorAppCancel.toLong()
                .toInt() ->
                SecurityError.AuthenticationCancelled

            LAError.LAErrorAuthenticationFailed.toLong()
                .toInt() ->
                SecurityError.AuthenticationFailed

            LAError.LAErrorBiometryNotAvailable.toLong()
                .toInt() ->
                SecurityError.BiometricNotAvailable

            LAError.LAErrorBiometryNotEnrolled.toLong()
                .toInt() ->
                SecurityError.BiometricNotEnrolled

            LAError.LAErrorBiometryLockout.toLong()
                .toInt() ->
                SecurityError.AuthenticationLockedOut

            LAError.LAErrorPasscodeNotSet.toLong()
                .toInt() ->
                SecurityError.PasscodeNotSet

            LAError.LAErrorUserFallback.toLong()
                .toInt() ->
                SecurityError.UserFallback

            else ->
                SecurityError.Unknown
        }
    }
}