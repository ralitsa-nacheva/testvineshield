package com.rncoding.testvineshield.core.di

import com.rncoding.testvineshield.core.domain.security.AndroidBiometricAuthenticator
import com.rncoding.testvineshield.core.domain.security.BiometricAuthenticator
import org.koin.dsl.module

// make android di lifecycle aware

val androidSecurityModule = module {

    factory<BiometricAuthenticator> {
        AndroidBiometricAuthenticator(
            activity = get()
        )
    }
}