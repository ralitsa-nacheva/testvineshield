package com.rncoding.testvineshield.core.domain.security

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.SecurityError
import com.rncoding.testvineshield.core.domain.security.BiometricAuthenticator
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicy
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class IosBiometricAuthenticator : BiometricAuthenticator {

    override fun isAvailable(): Boolean {

        val context = LAContext()

        return context
            .checkBiometricAvailability()
            .available
    }

    override suspend fun authenticate():
            Result<Unit, AppError> {

        val context = LAContext()

        context.localizedCancelTitle =
            "Cancel"

        /*
         * Check immediately before authentication.
         */
        val availability =
            context.checkBiometricAvailability()

        if (!availability.available) {

            val error =
                availability.error

            return Result.Error(
                if (error != null) {

                    IosBiometricErrorMapper.map(
                        error.code
                    )

                } else {

                    SecurityError.BiometricNotAvailable
                }
            )
        }

        return suspendCancellableCoroutine { continuation ->

            context.evaluatePolicy(
                policy =
                    LAPolicy.LAPolicyDeviceOwnerAuthenticationWithBiometrics, // set up xcode

                localizedReason =
                    "Authenticate to access your vineyard data."

            ) { success, error ->

                if (!continuation.isActive) {
                    return@evaluatePolicy
                }

                when {

                    success -> {
                        continuation.resume(
                            Result.Success(Unit)
                        )
                    }

                    error != null -> {
                        continuation.resume(
                            Result.Error(
                                IosBiometricErrorMapper.map(
                                    error.code
                                )
                            )
                        )
                    }

                    else -> {
                        continuation.resume(
                            Result.Error(
                                SecurityError.Unknown()
                            )
                        )
                    }
                }
            }

            continuation.invokeOnCancellation {
                context.invalidate()
            }
        }
    }
}