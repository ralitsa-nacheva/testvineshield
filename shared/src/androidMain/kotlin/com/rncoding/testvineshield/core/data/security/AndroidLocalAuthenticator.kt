package com.rncoding.testvineshield.core.data.security

import android.os.Looper
import android.os.Build
import android.app.KeyguardManager
import android.content.Context
import com.rncoding.testvineshield.core.domain.security.LocalUnlockMethod
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.SecurityError
import com.rncoding.testvineshield.core.domain.security.LocalAuthenticator
import com.rncoding.testvineshield.core.platform.AndroidActivityProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AndroidLocalAuthenticator(
    private val activityProvider: AndroidActivityProvider
) : LocalAuthenticator {
    // The platform binding is a singleton: all local-auth callers share this gate.
    private val promptMutex = Mutex()

    override suspend fun isAvailable(method: LocalUnlockMethod): Result<Boolean, AppError> {
        if (method == LocalUnlockMethod.Biometric) return isAvailable()
        return withContext(Dispatchers.Main.immediate) {
            val activity = activityProvider.getActivity() ?: return@withContext Result.Success(false)
            val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            Result.Success(!activity.isFinishing && !activity.isDestroyed && keyguard.isDeviceSecure)
        }
    }

    override suspend fun authenticate(reason: String, method: LocalUnlockMethod): Result<Unit, AppError> =
        authenticateWithMethod(reason, method)

    override suspend fun isAvailable(): Result<Boolean, AppError> =
        withContext(Dispatchers.Main.immediate) {
            val activity = activityProvider.getActivity()
                ?: return@withContext Result.Success(false)
            if (activity.isFinishing || activity.isDestroyed) {
                return@withContext Result.Success(false)
            }
            try {
                when (BiometricManager.from(activity).canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                )) {
                    BiometricManager.BIOMETRIC_SUCCESS -> Result.Success(true)
                    BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                        Result.Error(SecurityError.SecurityUpdateRequired)
                    else -> Result.Success(false)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()
                Result.Error(SecurityError.Unknown(exception))
            }
        }

    override suspend fun authenticate(reason: String): Result<Unit, AppError> =
        authenticateWithMethod(reason, LocalUnlockMethod.Biometric)

    private suspend fun authenticateWithMethod(reason: String, method: LocalUnlockMethod): Result<Unit, AppError> =
        withContext(Dispatchers.Main.immediate) {
            // Reject a second request; do not queue a surprise prompt for later.
            if (!promptMutex.tryLock()) {
                return@withContext Result.Error(SecurityError.AuthenticationInProgress)
            }
            try {
                val activity = activityProvider.getActivity()
                if (activity == null || !canPresentPrompt(activity)) {
                    return@withContext Result.Error(SecurityError.BiometricNotAvailable)
                }
                when (val availability = isAvailable(method)) {
                    is Result.Error -> return@withContext Result.Error(availability.error)
                    is Result.Success -> if (!availability.data) return@withContext Result.Error(
                        if (method == LocalUnlockMethod.Biometric) SecurityError.BiometricNotAvailable
                        else SecurityError.DeviceCredentialNotAvailable)
                }
                if (method == LocalUnlockMethod.DeviceCredential && Build.VERSION.SDK_INT < 30)
                    awaitLegacyCredential(activity, reason)
                else awaitPrompt(activity, reason, method)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()
                Result.Error(SecurityError.Unknown(exception))
            } finally {
                promptMutex.unlock()
            }
        }

    private fun isCurrentHost(activity: FragmentActivity): Boolean =
        !activity.isFinishing && !activity.isDestroyed &&
            activityProvider.getActivity() === activity

    private fun canPresentPrompt(activity: FragmentActivity): Boolean =
        isCurrentHost(activity) &&
            activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            !activity.supportFragmentManager.isStateSaved

    private suspend fun awaitPrompt(
        activity: FragmentActivity,
        reason: String,
        method: LocalUnlockMethod
    ): Result<Unit, AppError> {
        val executor = ContextCompat.getMainExecutor(activity)
        var prompt: BiometricPrompt? = null
        var lifecycleGuard: AndroidAuthenticationLifecycleGuard? = null
        // Read/write only on Main, including the posted cancellation action.
        var sessionOpen = true
        try {
            return suspendCancellableCoroutine { continuation ->
                fun finish(result: Result<Unit, AppError>) {
                    // Native terminal callbacks are serialized on the Main executor.
                    if (continuation.isActive) continuation.resume(result)
                }
                val currentPrompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            finish(
                                if (isCurrentHost(activity) &&
                                    activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                                ) Result.Success(Unit)
                                else Result.Error(SecurityError.AuthenticationCancelled)
                            )
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            finish(Result.Error(mapAuthenticationError(errorCode)))
                        }

                        override fun onAuthenticationFailed() {
                            // A non-matching scan is nonterminal; Android allows another try.
                        }
                    }
                )
                prompt = currentPrompt
                lifecycleGuard = AndroidAuthenticationLifecycleGuard(
                    activity.lifecycle,
                    cancelOnStop = method == LocalUnlockMethod.Biometric
                ) {
                    finish(Result.Error(SecurityError.AuthenticationCancelled))
                    currentPrompt.cancelAuthentication()
                }
                continuation.invokeOnCancellation {
                    val cancelOnMain = Runnable {
                        // Ignore delayed cleanup after this attempt's finally already ran.
                        if (sessionOpen) {
                            lifecycleGuard?.dispose()
                            currentPrompt.cancelAuthentication()
                        }
                    }
                    if (Looper.myLooper() == Looper.getMainLooper()) cancelOnMain.run()
                    else executor.execute(cancelOnMain)
                }
                if (continuation.isActive) {
                    val builder = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock Vineyard").setSubtitle(reason)
                    if (method == LocalUnlockMethod.Biometric) {
                        builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            .setNegativeButtonText("Cancel")
                    } else {
                        // DEVICE_CREDENTIAL alone is supported only from API 30.
                        builder.setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    }
                    currentPrompt.authenticate(builder.build())
                }
            }
        } finally {
            sessionOpen = false
            lifecycleGuard?.dispose()
            prompt?.cancelAuthentication()
        }
    }

    @Suppress("DEPRECATION") // API 24–29 compatible credential-only confirmation.
    private suspend fun awaitLegacyCredential(activity: FragmentActivity, reason: String): Result<Unit, AppError> {
        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val intent = keyguard.createConfirmDeviceCredentialIntent("Unlock Vineyard", reason)
            ?: return Result.Error(SecurityError.DeviceCredentialNotAvailable)
        var fragment: DeviceCredentialFragment? = null
        try {
            return suspendCancellableCoroutine { continuation ->
                val host = DeviceCredentialFragment()
                fragment = host
                host.onComplete = { success ->
                    if (continuation.isActive) continuation.resume(
                        if (success && isCurrentHost(activity) && activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) Result.Success(Unit)
                        else Result.Error(SecurityError.AuthenticationCancelled))
                }
                host.credentialIntent = intent
                activity.supportFragmentManager.beginTransaction().add(host, "VineShieldDeviceCredential").commitNow()
                continuation.invokeOnCancellation {
                    ContextCompat.getMainExecutor(activity).execute { host.cancel() }
                }
            }
        } finally { fragment?.cancel() }
    }

    private fun mapAuthenticationError(errorCode: Int): SecurityError = when (errorCode) {
        BiometricPrompt.ERROR_CANCELED,
        BiometricPrompt.ERROR_USER_CANCELED,
        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> SecurityError.AuthenticationCancelled
        BiometricPrompt.ERROR_LOCKOUT -> SecurityError.AuthenticationLockedOut
        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> SecurityError.AuthenticationPermanentlyLockedOut
        BiometricPrompt.ERROR_NO_BIOMETRICS -> SecurityError.BiometricNotEnrolled
        BiometricPrompt.ERROR_HW_NOT_PRESENT,
        BiometricPrompt.ERROR_HW_UNAVAILABLE -> SecurityError.BiometricNotAvailable
        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> SecurityError.DeviceCredentialNotAvailable
        BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED -> SecurityError.SecurityUpdateRequired
        else -> SecurityError.AuthenticationFailed
    }
}
