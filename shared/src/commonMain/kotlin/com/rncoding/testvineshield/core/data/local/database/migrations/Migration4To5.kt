package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Preserve the block parent/child graph: change columns, never replace/delete the parent table. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        // Legacy fields were non-null Ints. Refuse affinity-corrupt text instead of CASTing it to zero.
        val invalid = connection.prepare("SELECT 1 FROM block WHERE typeof(area) != 'integer' OR area < -2147483648 OR area > 2147483647 OR typeof(row_spacing) != 'integer' OR row_spacing < -2147483648 OR row_spacing > 2147483647 OR typeof(row_count) != 'integer' OR row_count < -2147483648 OR row_count > 2147483647 OR typeof(planted_at) != 'text' LIMIT 1")
        try { check(!invalid.step()) { "Unexpected legacy block storage; migration stopped without replacing records." } }
        finally { invalid.close() }
        connection.execSQL("ALTER TABLE block ADD COLUMN area_decimal_value REAL")
        connection.execSQL("ALTER TABLE block ADD COLUMN row_spacing_decimal_value REAL")
        connection.execSQL("ALTER TABLE block ADD COLUMN row_count_optional_value INTEGER")
        connection.execSQL("ALTER TABLE block ADD COLUMN planted_at_optional_value TEXT")
        connection.execSQL("UPDATE block SET area_decimal_value=CAST(area AS REAL), row_spacing_decimal_value=CAST(row_spacing AS REAL), row_count_optional_value=row_count, planted_at_optional_value=planted_at")
        connection.execSQL("ALTER TABLE block DROP COLUMN area")
        connection.execSQL("ALTER TABLE block DROP COLUMN row_spacing")
        connection.execSQL("ALTER TABLE block DROP COLUMN row_count")
        connection.execSQL("ALTER TABLE block DROP COLUMN planted_at")
        connection.execSQL("ALTER TABLE block RENAME COLUMN area_decimal_value TO area")
        connection.execSQL("ALTER TABLE block RENAME COLUMN row_spacing_decimal_value TO row_spacing")
        connection.execSQL("ALTER TABLE block RENAME COLUMN row_count_optional_value TO row_count")
        connection.execSQL("ALTER TABLE block RENAME COLUMN planted_at_optional_value TO planted_at")
    }
}
