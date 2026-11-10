package com.rncoding.testvineshield.core.data.local.database

import com.rncoding.testvineshield.core.data.local.database.migrations.MIGRATION_3_4
import com.rncoding.testvineshield.core.data.local.database.migrations.MIGRATION_2_3
import com.rncoding.testvineshield.core.data.local.database.migrations.MIGRATION_1_2
import com.rncoding.testvineshield.core.data.local.database.migrations.MIGRATION_4_5
import com.rncoding.testvineshield.core.data.local.database.migrations.MIGRATION_6_7
import com.rncoding.testvineshield.core.data.local.database.migrations.MIGRATION_5_6
import com.rncoding.testvineshield.core.data.local.database.migrations.MIGRATION_7_8
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun buildVineshieldDatabase(
    builder: RoomDatabase.Builder<VineshieldDatabase>
): VineshieldDatabase {

    return builder
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
        .setDriver(
            BundledSQLiteDriver()
        )
        .setQueryCoroutineContext(
            Dispatchers.IO
        )
        .build()
}