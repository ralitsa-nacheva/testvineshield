package com.rncoding.testvineshield.core.domain.security

data class SecuritySettings(
    val biometricEnabled: Boolean,
    val pinEnabled: Boolean,
    val requireUnlockOnLaunch: Boolean,
    val inactivityTimeoutMillis: Long
)