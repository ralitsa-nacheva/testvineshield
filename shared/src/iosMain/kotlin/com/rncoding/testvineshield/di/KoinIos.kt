package com.rncoding.testvineshield.di

fun initKoinIos() {
    initKoin(
        platformModules = listOf(
            iosModule
        )
    )
}