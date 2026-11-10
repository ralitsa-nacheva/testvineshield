package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Absence is NULL; never reinterpret the old activity's untyped rate. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE spray_product ADD COLUMN rate_quantity REAL")
        connection.execSQL("ALTER TABLE spray_product ADD COLUMN rate_product_unit TEXT")
        connection.execSQL("ALTER TABLE spray_product ADD COLUMN rate_basis_quantity REAL")
        connection.execSQL("ALTER TABLE spray_product ADD COLUMN rate_basis_unit TEXT")
    }
}
