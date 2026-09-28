package com.rncoding.testvineshield.di

import com.rncoding.testvineshield.core.data.security.IosLocalAuthenticator
import com.rncoding.testvineshield.core.data.security.IosPasswordHasher
import com.rncoding.testvineshield.core.data.security.IosSecureStorage
import com.rncoding.testvineshield.core.domain.security.LocalAuthenticator
import com.rncoding.testvineshield.core.domain.security.PasswordHasher
import com.rncoding.testvineshield.core.domain.security.SecureStorage
import org.koin.dsl.module

val iosModule =
    module {

        single<PasswordHasher> {
            IosPasswordHasher()
        }

        single<SecureStorage> {
            IosSecureStorage()
        }

        single<LocalAuthenticator> {
            IosLocalAuthenticator()
        }
    }