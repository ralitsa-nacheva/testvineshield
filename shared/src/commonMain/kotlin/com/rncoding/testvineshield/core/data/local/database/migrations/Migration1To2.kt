package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Add metadata only. Existing numeric values and relationships are never converted. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE vineyard ADD COLUMN size_unit TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE block ADD COLUMN area_unit TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE block ADD COLUMN row_spacing_unit TEXT DEFAULT NULL")
    }
}
