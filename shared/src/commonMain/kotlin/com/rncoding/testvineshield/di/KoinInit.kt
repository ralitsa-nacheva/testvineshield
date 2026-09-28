package com.rncoding.testvineshield.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initKoin(
    appDeclaration: KoinApplication.() -> Unit = {},
    platformModules: List<Module> = emptyList()
) {
    startKoin {
        appDeclaration()

        modules(
            commonModule,
            *platformModules.toTypedArray()
        )
    }
}