package com.rncoding.testvineshield.core.domain.datamodels

data class SecuritySettings(
    val biometricEnabled: Boolean = false,
    val pinEnabled: Boolean = false
)