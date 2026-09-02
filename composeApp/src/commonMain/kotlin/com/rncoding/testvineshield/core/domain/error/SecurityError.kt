package com.rncoding.testvineshield.core.domain.error

sealed interface SecurityError : AppError {

    data object BiometricNotAvailable : SecurityError {

        override val userMessage =
            "Biometric authentication is not available on this device."

        override val debugMessage =
            "BiometricManager reported that the requested biometric authenticators are unavailable."

        override val cause: Throwable? = null
    }

    data object BiometricNotEnrolled : SecurityError {

        override val userMessage =
            "No biometric is enrolled on this device. Set up fingerprint or face authentication in device settings."

        override val debugMessage =
            "The device supports biometric authentication, but no biometric credentials are enrolled."

        override val cause: Throwable? = null
    }

    data object AuthenticationFailed : SecurityError {

        override val userMessage =
            "Biometric authentication failed."

        override val debugMessage =
            "The biometric authentication attempt failed."

        override val cause: Throwable? = null
    }

    data object AuthenticationCancelled : SecurityError {

        override val userMessage =
            "Biometric authentication was cancelled."

        override val debugMessage =
            "The user cancelled the biometric authentication prompt."

        override val cause: Throwable? = null
    }

    data object AuthenticationLockedOut : SecurityError {

        override val userMessage =
            "Biometric authentication is temporarily locked. Try again later or use your PIN."

        override val debugMessage =
            "Android biometric authentication entered temporary lockout."

        override val cause: Throwable? = null
    }

    data object AuthenticationPermanentlyLockedOut : SecurityError {

        override val userMessage =
            "Biometric authentication is disabled until you unlock your device."

        override val debugMessage =
            "Android biometric authentication entered permanent lockout."

        override val cause: Throwable? = null
    }

    data object SecurityUpdateRequired : SecurityError {

        override val userMessage =
            "Biometric authentication is temporarily unavailable because the device requires a security update."

        override val debugMessage =
            "Biometric authentication entered temporary lockout.\"."

        override val cause: Throwable? = null
    }

    data object NoDeviceCredential : SecurityError {

        override val userMessage =
            "No device PIN, pattern, or password is configured."

        override val debugMessage =
            "Device credential authentication was requested but no secure device credential exists."

        override val cause: Throwable? = null
    }

    data object PasscodeNotSet : SecurityError {

        override val userMessage =
            "A device passcode is required."

        override val debugMessage =
            "The device does not have a passcode configured."

        override val cause: Throwable? = null
    }

    data class Unknown(
        override val cause: Throwable? = null
    ) : SecurityError {

        override val userMessage =
            "Biometric authentication could not be completed."

        override val debugMessage =
            "Unexpected biometric authentication error."
    }
}