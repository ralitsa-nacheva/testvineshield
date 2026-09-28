package com.rncoding.testvineshield.di

import com.rncoding.testvineshield.di.iosModule
import com.rncoding.testvineshield.di.iosDatabaseModule

fun initKoinIos() {
    initKoin(
        platformModules = listOf(
            iosModule,
            iosDatabaseModule
        )
    )
}