package com.rncoding.testvineshield.core.data.security

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.SecurityError
import com.rncoding.testvineshield.core.domain.security.LocalAuthenticator
import com.rncoding.testvineshield.core.domain.security.LocalUnlockMethod
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import platform.Foundation.NSError
import platform.LocalAuthentication.*
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosLocalAuthenticator : LocalAuthenticator {
    private val promptLock = Mutex()
    private var pendingContext: LAContext? = null

    override suspend fun isAvailable(): Result<Boolean, AppError> = isAvailable(LocalUnlockMethod.Biometric)

    override suspend fun isAvailable(method: LocalUnlockMethod): Result<Boolean, AppError> = withContext(Dispatchers.Main.immediate) {
        val context = LAContext()
        try {
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                error.value = null
                if (context.canEvaluatePolicy(policy(method), error.ptr)) {
                    Result.Success(true)
                } else when (error.value?.code) {
                    LAErrorPasscodeNotSet -> Result.Error(SecurityError.PasscodeNotSet)
                    else -> Result.Success(false)
                }
            }
        } finally { context.invalidate() }
    }

    override suspend fun authenticate(reason: String): Result<Unit, AppError> =
        authenticate(reason, LocalUnlockMethod.Biometric)

    override suspend fun authenticate(reason: String, method: LocalUnlockMethod): Result<Unit, AppError> =
        withContext(Dispatchers.Main.immediate) {
            if (!promptLock.tryLock()) return@withContext Result.Error(SecurityError.AuthenticationInProgress)
            val context = LAContext()
            pendingContext = context
            try {
                suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation { context.invalidate() }
                    context.evaluatePolicy(
                        policy(method),
                        localizedReason = reason
                    ) { success, error ->
                        // LA callbacks use an unspecified queue. Only Main may resume/inspect state.
                        launch(Dispatchers.Main.immediate) {
                            if (continuation.isActive) continuation.resume(
                                if (success) Result.Success(Unit)
                                else Result.Error(mapError(error))
                            )
                        }
                    }
                }
            } finally {
                if (pendingContext === context) pendingContext = null
                context.invalidate()
                promptLock.unlock()
            }
        }

    // iOS owner authentication permits system-selected biometric/passcode fallback.
    private fun policy(method: LocalUnlockMethod) = when (method) {
        LocalUnlockMethod.Biometric -> LAPolicyDeviceOwnerAuthenticationWithBiometrics
        LocalUnlockMethod.DeviceCredential -> LAPolicyDeviceOwnerAuthentication
    }

    /** Background only: an inactive biometric overlay must not cancel its own prompt. */
    fun cancelPendingAuthentication() { pendingContext?.invalidate() }

    private fun mapError(error: NSError?): SecurityError {
        if (error?.domain != LAErrorDomain) return SecurityError.Unknown()
        return when (error.code) {
            LAErrorUserCancel, LAErrorSystemCancel, LAErrorAppCancel -> SecurityError.AuthenticationCancelled
            LAErrorAuthenticationFailed -> SecurityError.AuthenticationFailed
            LAErrorBiometryNotAvailable -> SecurityError.BiometricNotAvailable
            LAErrorBiometryNotEnrolled -> SecurityError.BiometricNotEnrolled
            LAErrorBiometryLockout -> SecurityError.AuthenticationLockedOut
            LAErrorPasscodeNotSet -> SecurityError.PasscodeNotSet
            LAErrorUserFallback -> SecurityError.UserFallback
            else -> SecurityError.Unknown()
        }
    }
}
