package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE security_settings ADD COLUMN inactivity_lock_enabled INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE security_settings ADD COLUMN inactivity_timeout_millis INTEGER NOT NULL DEFAULT 900000")
        connection.execSQL("ALTER TABLE security_settings ADD COLUMN device_credential_enabled INTEGER NOT NULL DEFAULT 0")
        // Old switches did not control inactivity locking. Require fresh, deliberate opt-in.
        connection.execSQL("UPDATE security_settings SET biometric_enabled = 0, app_pin_enabled = 0")
    }
}
