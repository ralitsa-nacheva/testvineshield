package com.rncoding.testvineshield

import com.rncoding.testvineshield.core.data.scheduler.ForegroundWeatherHistoryRiskScheduler
import com.rncoding.testvineshield.core.data.security.IosLocalAuthenticator
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/** Swift calls these methods on the main thread. Inactive includes biometric overlays. */
object IosAppLifecycle : KoinComponent {
    fun onForeground() { get<ForegroundWeatherHistoryRiskScheduler>().setForeground(true) }
    fun onInactive() { get<ForegroundWeatherHistoryRiskScheduler>().setForeground(false) }
    fun onBackground() {
        onInactive()
        get<IosLocalAuthenticator>().cancelPendingAuthentication()
    }
}
