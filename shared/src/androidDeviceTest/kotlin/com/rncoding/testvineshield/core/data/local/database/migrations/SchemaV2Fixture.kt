package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v2 shape is archived v1 + production 1->2. Identity below was read from the user's
 * compiler-generated v2 Room delegate in the supplied installed APK, not invented.
 */
internal object SchemaV2Fixture {
    const val ROOM_IDENTITY = "5c637286780bb274bf077929d2076f09"
    val columns = SchemaV1Fixture.columns.mapValues { (table, columns) -> when (table) {
        "vineyard" -> columns + "size_unit"
        "block" -> columns + listOf("area_unit", "row_spacing_unit")
        else -> columns
    } }
    fun create(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        SchemaV1Fixture.statements.forEach { connection.execSQL(it) }
        MIGRATION_1_2.migrate(connection)
        connection.execSQL("UPDATE room_master_table SET identity_hash = '$ROOM_IDENTITY' WHERE id = 42")
        connection.execSQL("PRAGMA user_version = 2")
        connection.execSQL("UPDATE vineyard SET size_unit = 'ha' WHERE vineyard_id = 10")
        connection.execSQL("UPDATE block SET area_unit = 'm2', row_spacing_unit = 'cm' WHERE block_id = 20")
        connection.execSQL("INSERT INTO vineyard SELECT 11,user_id,'Negative',size,country,city,latitude,longitude,time_zone,-125,4,created_at,updated_at,size_unit FROM vineyard WHERE vineyard_id=10")
        connection.execSQL("INSERT INTO vineyard SELECT 12,user_id,'Zero',size,country,city,latitude,longitude,time_zone,0,5,created_at,updated_at,size_unit FROM vineyard WHERE vineyard_id=10")
        connection.execSQL("INSERT INTO vineyard SELECT 900,user_id,'Deleted high id',size,country,city,latitude,longitude,time_zone,200,6,created_at,updated_at,size_unit FROM vineyard WHERE vineyard_id=10")
        connection.execSQL("DELETE FROM vineyard WHERE vineyard_id=900")
    }
}
