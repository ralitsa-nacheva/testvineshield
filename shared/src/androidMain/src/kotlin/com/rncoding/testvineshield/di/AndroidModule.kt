package com.rncoding.testvineshield.core.di

import com.rncoding.testvineshield.core.data.security.AndroidLocalAuthenticator
import com.rncoding.testvineshield.core.data.security.AndroidPasswordHasher
import com.rncoding.testvineshield.core.data.security.AndroidSecureStorage
import com.rncoding.testvineshield.core.domain.security.LocalAuthenticator
import com.rncoding.testvineshield.core.domain.security.PasswordHasher
import com.rncoding.testvineshield.core.domain.security.SecureStorage
import com.rncoding.testvineshield.core.platform.AndroidActivityProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule =
    module {

        single {
            AndroidActivityProvider()
        }

        single<PasswordHasher> {
            AndroidPasswordHasher()
        }

        single<SecureStorage> {
            AndroidSecureStorage(
                context = androidContext()
            )
        }

        single<LocalAuthenticator> {
            AndroidLocalAuthenticator(
                activityProvider = get()
            )
        }
    }