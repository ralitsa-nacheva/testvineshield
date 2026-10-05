package com.rncoding.testvineshield.di

import android.content.Context
import org.koin.android.ext.koin.androidContext

fun initKoinAndroid(
    context: Context
) {
    initKoin(
        appDeclaration = {
            androidContext(
                context.applicationContext
            )
        },
        platformModules = listOf(
            androidModule
        )
    )
}