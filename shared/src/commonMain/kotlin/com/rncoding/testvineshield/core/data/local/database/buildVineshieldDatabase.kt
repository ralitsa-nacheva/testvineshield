package com.rncoding.testvineshield.core.data.local.database

import com.rncoding.testvineshield.core.data.local.database.migrations.MIGRATION_1_2
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun buildVineshieldDatabase(
    builder: RoomDatabase.Builder<VineshieldDatabase>
): VineshieldDatabase {

    return builder
        .addMigrations(MIGRATION_1_2)
        .setDriver(
            BundledSQLiteDriver()
        )
        .setQueryCoroutineContext(
            Dispatchers.IO
        )
        .build()
}