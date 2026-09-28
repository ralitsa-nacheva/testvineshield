package com.rncoding.testvineshield.di

import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.buildVineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.getVineshieldDatabaseBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidDatabaseModule =
    module {

        single<VineshieldDatabase> {

            buildVineshieldDatabase(
                getVineshieldDatabaseBuilder(
                    context = androidContext()
                )
            )
        }
    }