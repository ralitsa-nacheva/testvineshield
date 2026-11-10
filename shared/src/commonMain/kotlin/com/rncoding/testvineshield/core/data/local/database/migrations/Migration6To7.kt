package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Unmeasured Brix is NULL; preserve every historical value, including zero. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE harvest ADD COLUMN brix_sugar_content_optional REAL")
        connection.execSQL("UPDATE harvest SET brix_sugar_content_optional=brix_sugar_content")
        connection.execSQL("ALTER TABLE harvest DROP COLUMN brix_sugar_content")
        connection.execSQL("ALTER TABLE harvest RENAME COLUMN brix_sugar_content_optional TO brix_sugar_content")
    }
}
