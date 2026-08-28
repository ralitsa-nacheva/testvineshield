package com.rncoding.testvineshield.core.domain.security

object SessionPolicy {
    // Hard logout after 7 days
    const val MAX_SESSION_DURATION = 7 * 24 * 60 * 60 * 1000L

    // Soft timeout after inactivity (30 min)
    const val INACTIVITY_TIMEOUT = 30 * 60 * 1000L

    // Require re-auth (biometric/PIN) after 5 min
    const val REAUTH_THRESHOLD = 5 * 60 * 1000L
}