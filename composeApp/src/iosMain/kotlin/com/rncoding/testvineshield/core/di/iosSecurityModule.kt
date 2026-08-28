package com.rncoding.testvineshield.core.di

import com.rncoding.testvineshield.core.domain.security.BiometricAuthenticator
import com.rncoding.testvineshield.core.domain.security.IosBiometricAuthenticator
import org.koin.dsl.module

val iosSecurityModule =
    module {

        single<BiometricAuthenticator> {
            IosBiometricAuthenticator()
        }
    }