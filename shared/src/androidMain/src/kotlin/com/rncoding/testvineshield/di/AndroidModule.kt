package com.rncoding.testvineshield.di

import com.rncoding.testvineshield.core.data.local.database.AndroidDatabaseFactory
import com.rncoding.testvineshield.core.data.local.database.DatabaseFactory
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.buildVineshieldDatabase
import com.rncoding.testvineshield.core.data.remote.AndroidHttpClientFactory
import com.rncoding.testvineshield.core.data.remote.HttpClientFactory
import com.rncoding.testvineshield.core.data.security.AndroidLocalAuthenticator
import com.rncoding.testvineshield.core.data.security.AndroidPasswordHasher
import com.rncoding.testvineshield.core.data.security.AndroidSecureStorage
import com.rncoding.testvineshield.core.domain.security.LocalAuthenticator
import com.rncoding.testvineshield.core.domain.security.PasswordHasher
import com.rncoding.testvineshield.core.domain.security.SecureStorage
import com.rncoding.testvineshield.core.platform.AndroidActivityProvider
import io.ktor.client.HttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule =
    module {

        /*
         * Database
         */

        single<DatabaseFactory> {
            AndroidDatabaseFactory(
                context = androidContext()
            )
        }

        single<VineshieldDatabase> {
            buildVineshieldDatabase(
                builder = get<DatabaseFactory>().create()
            )
        }

        /*
         * Networking
         */

        single<HttpClientFactory> {
            AndroidHttpClientFactory()
        }

        single<HttpClient> {
            get<HttpClientFactory>().create()
        }

        /*
         * Activity access
         */

        single {
            AndroidActivityProvider()
        }

        /*
         * Security
         */

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