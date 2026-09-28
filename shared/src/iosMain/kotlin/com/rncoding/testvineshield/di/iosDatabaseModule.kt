package com.rncoding.testvineshield.di

import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.buildVineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.getVineshieldDatabaseBuilder
import org.koin.dsl.module

val iosDatabaseModule =
    module {

        single<VineshieldDatabase> {

            buildVineshieldDatabase(
                getVineshieldDatabaseBuilder()
            )
        }
    }