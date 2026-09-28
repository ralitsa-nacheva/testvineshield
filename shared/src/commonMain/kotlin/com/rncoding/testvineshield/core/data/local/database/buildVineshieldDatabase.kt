package com.rncoding.testvineshield.core.data.local.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun buildVineshieldDatabase(
    builder: RoomDatabase.Builder<VineshieldDatabase>
): VineshieldDatabase {

    return builder
        .setDriver(
            BundledSQLiteDriver()
        )
        .setQueryCoroutineContext(
            Dispatchers.IO
        )
        .build()
}