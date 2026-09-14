package com.rncoding.testvineshield.core.data.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.SecurityError
import com.rncoding.testvineshield.core.domain.security.LocalAuthenticator
import com.rncoding.testvineshield.core.platform.AndroidActivityProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidLocalAuthenticator(
    private val activityProvider:
    AndroidActivityProvider
) : LocalAuthenticator {

    override suspend fun isAvailable():
            Result<Boolean, AppError> {

        val activity =
            activityProvider.getActivity()
                ?: return Result.Success(false)

        val biometricManager =
            BiometricManager.from(activity)

        return when (
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
        ) {

            BiometricManager.BIOMETRIC_SUCCESS ->
                Result.Success(true)

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Result.Success(false)

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                Result.Error(
                    SecurityError.SecurityUpdateRequired
                )

            else ->
                Result.Success(false)
        }
    }

    override suspend fun authenticate(
        reason: String
    ): Result<Unit, AppError> {

        val activity =
            activityProvider.getActivity()
                ?: return Result.Error(
                    SecurityError.BiometricNotAvailable
                )

        return suspendCancellableCoroutine { continuation ->

            val executor =
                activity.mainExecutor

            val prompt =
                BiometricPrompt(
                    activity,
                    executor,
                    object :
                        BiometricPrompt.AuthenticationCallback() {

                        override fun
                                onAuthenticationSucceeded(
                            result:
                            BiometricPrompt.AuthenticationResult
                        ) {
                            if (
                                continuation.isActive
                            ) {
                                continuation.resume(
                                    Result.Success(Unit)
                                )
                            }
                        }

                        override fun
                                onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            if (
                                !continuation.isActive
                            ) {
                                return
                            }

                            continuation.resume(
                                Result.Error(
                                    mapAuthenticationError(
                                        errorCode
                                    )
                                )
                            )
                        }

                        override fun
                                onAuthenticationFailed() {
                            // Do not finish the
                            // coroutine here.
                            //
                            // Android may allow the
                            // user to try again.
                        }
                    }
                )

            val promptInfo =
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(
                        "Unlock Vineyard"
                    )
                    .setSubtitle(reason)
                    .setNegativeButtonText(
                        "Cancel"
                    )
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators
                            .BIOMETRIC_STRONG
                    )
                    .build()

            prompt.authenticate(
                promptInfo
            )

            continuation.invokeOnCancellation {
                prompt.cancelAuthentication()
            }
        }
    }

    private fun mapAuthenticationError(
        errorCode: Int
    ): SecurityError {

        return when (errorCode) {

            BiometricPrompt.ERROR_CANCELED,
            BiometricPrompt.ERROR_USER_CANCELED ->
                SecurityError.AuthenticationCancelled

            BiometricPrompt.ERROR_NEGATIVE_BUTTON ->
                SecurityError.UserFallback

            BiometricPrompt.ERROR_LOCKOUT ->
                SecurityError.AuthenticationLockedOut

            BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                SecurityError.AuthenticationPermanentlyLockedOut

            BiometricPrompt.ERROR_NO_BIOMETRICS ->
                SecurityError.BiometricNotEnrolled

            BiometricPrompt.ERROR_HW_NOT_PRESENT ->
                SecurityError.BiometricNotAvailable

            BiometricPrompt.ERROR_HW_UNAVAILABLE ->
                SecurityError.BiometricNotAvailable

            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ->
                SecurityError.DeviceCredentialNotAvailable

            BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED ->
                SecurityError.SecurityUpdateRequired

            else ->
                SecurityError.AuthenticationFailed
        }
    }
}