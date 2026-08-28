package com.rncoding.testvineshield.core.domain.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicy

private val Long.Companion.LAPolicyDeviceOwnerAuthenticationWithBiometrics // set up x code

internal data class IosBiometricAvailability(
    val available: Boolean,
    val error: NSError?
)

@OptIn(ExperimentalForeignApi::class)
internal fun LAContext.checkBiometricAvailability():
        IosBiometricAvailability {

    return memScoped {

        val error =
            alloc<ObjCObjectVar<NSError?>>()

        val available =
            canEvaluatePolicy(
                LAPolicy.LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                error.ptr
            )

        IosBiometricAvailability(
            available = available,
            error = error.value
        )
    }
}