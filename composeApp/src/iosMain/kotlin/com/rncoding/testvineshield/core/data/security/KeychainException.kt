package com.rncoding.testvineshield.core.data.security

import platform.darwin.OSStatus

class KeychainException(
    val status: OSStatus
) : Exception("Keychain error with status: $status")