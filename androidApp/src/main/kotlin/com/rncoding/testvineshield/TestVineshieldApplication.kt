package com.rncoding.testvineshield

import android.app.Application
import com.rncoding.testvineshield.di.initKoin
import org.koin.android.ext.koin.androidContext

class TestVineshieldApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@TestVineshieldApplication)
        }
    }
}