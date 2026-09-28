package com.rncoding.testvineshield

import android.app.Application
import com.rncoding.testvineshield.di.initKoinAndroid

class TestVineshieldApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoinAndroid(this)
    }
}