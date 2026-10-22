
package com.rncoding.testvineshield

import android.app.Application
import androidx.work.Configuration
import com.rncoding.testvineshield.core.worker.WeatherHistoryWorkerFactory
import com.rncoding.testvineshield.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

class TestVineshieldApplication :
    Application(),
    Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@TestVineshieldApplication)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(
                WeatherHistoryWorkerFactory(
                    koin = GlobalContext.get()
                )
            )
            .build()
}
