package com.rncoding.testvineshield.core.domain.security

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.SecurityError
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidBiometricAuthenticator(
    private val activityProvider: () -> FragmentActivity
) : BiometricAuthenticator {

    private val activity: FragmentActivity
        get() = activityProvider()

    private companion object {
        const val AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG
    }

    override fun isAvailable(): Boolean {

        return BiometricManager
            .from(activity)
            .canAuthenticate(AUTHENTICATORS) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun authenticate():
            Result<Unit, AppError> {

        val manager =
            BiometricManager.from(activity)

        val availability =
            manager.canAuthenticate(
                AUTHENTICATORS
            )

        AndroidBiometricErrorMapper
            .mapAvailability(availability)
            ?.let { error ->
                return Result.Error(error)
            }

        return suspendCancellableCoroutine { continuation ->


            val executor =
                activity.mainExecutor

            val prompt =
                BiometricPrompt(
                    activity,
                    executor,
                    object :
                        BiometricPrompt.AuthenticationCallback() {

                        override fun onAuthenticationSucceeded(
                            result:
                            BiometricPrompt.AuthenticationResult
                        ) {
                            if (continuation.isActive) {
                                continuation.resume(
                                    Result.Success(Unit)
                                )
                            }
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            if (continuation.isActive) {

                                val error =
                                    AndroidBiometricErrorMapper
                                        .mapAuthentication(
                                            errorCode
                                        )

                                continuation.resume(
                                    Result.Error(error)
                                )
                            }
                        }

                        override fun onAuthenticationFailed() {
                            // Do not resume.
                            //
                            // This callback represents a failed
                            // biometric attempt, not necessarily
                            // termination of the authentication flow.
                        }
                    }
                )

            val promptInfo =
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Vineyard")
                    .setSubtitle(
                        "Authenticate to access your vineyard data"
                    )
                    .setDescription(
                        "Use your fingerprint or face to unlock the application."
                    )
                    .setAllowedAuthenticators(
                        AUTHENTICATORS
                    )
                    .setNegativeButtonText(
                        "Cancel"
                    )
                    .build()

            prompt.authenticate(promptInfo)

            continuation.invokeOnCancellation {
                prompt.cancelAuthentication()
            }
        }
    }
}