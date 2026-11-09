package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Unknown stage is NULL, never a fabricated dormancy/flowering observation. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE activity ADD COLUMN phenological_stage_optional TEXT")
        connection.execSQL("UPDATE activity SET phenological_stage_optional=phenological_stage")
        connection.execSQL("ALTER TABLE activity DROP COLUMN phenological_stage")
        connection.execSQL("ALTER TABLE activity RENAME COLUMN phenological_stage_optional TO phenological_stage")
    }
}
