package com.rncoding.testvineshield.core.domain.security

/** Account password is always available; optional OS methods never replace it. */
data class SecuritySettings(
    val biometricEnabled: Boolean = false,
    // Reserved legacy field. No app-specific PIN was ever implemented.
    val appPinEnabled: Boolean = false,
    val inactivityLockEnabled: Boolean = false,
    val inactivityTimeoutMillis: Long = 15 * 60_000L,
    val deviceCredentialEnabled: Boolean = false
) {
    fun normalized(): SecuritySettings = copy(
        appPinEnabled = false,
        biometricEnabled = inactivityLockEnabled && biometricEnabled,
        deviceCredentialEnabled = inactivityLockEnabled && deviceCredentialEnabled
    )
    companion object {
        val timeoutMinutes = listOf(1, 2, 5, 10, 15, 30, 60)
        fun validTimeout(value: Long) = timeoutMinutes.any { it * 60_000L == value }
    }
}
