package com.rncoding.testvineshield.core.domain.security
import kotlin.test.*
class InactivityPolicyTest {
    @Test fun defaultsAreOff() { val s = SecuritySettings(); assertFalse(s.inactivityLockEnabled); assertFalse(s.biometricEnabled); assertFalse(s.deviceCredentialEnabled) }
    @Test fun disablingClearsAllOptionalMethods() {
        val s = SecuritySettings(true, true, true, 60_000, true).copy(inactivityLockEnabled = false).normalized()
        assertFalse(s.biometricEnabled); assertFalse(s.appPinEnabled); assertFalse(s.deviceCredentialEnabled)
    }
    @Test fun reEnablingDoesNotRestoreOldMethods() {
        val off = SecuritySettings(true, false, true, 60_000, true).copy(inactivityLockEnabled = false).normalized()
        val on = off.copy(inactivityLockEnabled = true).normalized()
        assertTrue(on.inactivityLockEnabled); assertFalse(on.biometricEnabled); assertFalse(on.deviceCredentialEnabled)
    }
    @Test fun onlyBoundedTimeoutsAreValid() {
        SecuritySettings.timeoutMinutes.forEach { assertTrue(SecuritySettings.validTimeout(it * 60_000L)) }
        listOf(-1L, 0L, 1L, 59_999L, Long.MAX_VALUE).forEach { assertFalse(SecuritySettings.validTimeout(it)) }
    }
}
