package com.rncoding.testvineshield.di

import com.rncoding.testvineshield.core.data.local.database.DatabaseFactory
import com.rncoding.testvineshield.core.data.local.database.IosDatabaseFactory
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.buildVineshieldDatabase
import com.rncoding.testvineshield.core.data.remote.HttpClientFactory
import com.rncoding.testvineshield.core.data.remote.IosHttpClientFactory
import com.rncoding.testvineshield.core.data.security.IosLocalAuthenticator
import com.rncoding.testvineshield.core.data.security.IosPasswordHasher
import com.rncoding.testvineshield.core.data.security.IosSecureStorage
import com.rncoding.testvineshield.core.domain.security.LocalAuthenticator
import com.rncoding.testvineshield.core.domain.security.PasswordHasher
import com.rncoding.testvineshield.core.domain.security.SecureStorage
import io.ktor.client.HttpClient
import org.koin.dsl.module

val iosModule =
    module {

        /*
         * Database
         */

        single<DatabaseFactory> {
            IosDatabaseFactory()
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
            IosHttpClientFactory()
        }

        single<HttpClient> {
            get<HttpClientFactory>().create()
        }

        /*
         * Security
         */

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