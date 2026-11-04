package com.rncoding.testvineshield.di

import com.rncoding.testvineshield.core.data.scheduler.ForegroundWeatherHistoryRiskScheduler
import com.rncoding.testvineshield.core.data.security.IosLocalAuthenticator
import com.rncoding.testvineshield.core.domain.scheduler.WeatherHistoryRiskScheduler
import com.rncoding.testvineshield.core.domain.security.LocalAuthenticator
import org.koin.dsl.koinApplication
import kotlin.test.*

class IosModuleTest {
    @Test fun nativeBindingsResolveWithoutEagerSessionDependencies() {
        // No common module: resolving the scheduler must not eagerly request session/database.
        val application = koinApplication { modules(iosModule) }
        try {
            val koin = application.koin
            assertSame(koin.get<ForegroundWeatherHistoryRiskScheduler>(), koin.get<WeatherHistoryRiskScheduler>())
            assertSame(koin.get<IosLocalAuthenticator>(), koin.get<LocalAuthenticator>())
        } finally { application.close() }
    }
}
