package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Bundled SQLite supports column drop/rename; do not replace the parent table and cascade children. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        // SQLite affinity permits unexpected TEXT. Never silently cast corrupt legacy data to zero.
        val invalid = connection.prepare("SELECT 1 FROM vineyard WHERE typeof(elevation) != 'integer' OR elevation < -2147483648 OR elevation > 2147483647 LIMIT 1")
        try {
            check(!invalid.step()) { "Unexpected legacy elevation storage; migration stopped without replacing records." }
        } finally { invalid.close() }
        connection.execSQL("ALTER TABLE vineyard ADD COLUMN elevation_decimal_m REAL")
        connection.execSQL("UPDATE vineyard SET elevation_decimal_m = CAST(elevation AS REAL)")
        connection.execSQL("ALTER TABLE vineyard DROP COLUMN elevation")
        connection.execSQL("ALTER TABLE vineyard RENAME COLUMN elevation_decimal_m TO elevation")
    }
}
