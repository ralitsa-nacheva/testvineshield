package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE block ADD COLUMN stage_observed_on TEXT DEFAULT NULL")
    }
}
