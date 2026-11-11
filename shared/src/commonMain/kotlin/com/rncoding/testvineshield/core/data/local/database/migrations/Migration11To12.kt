package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE block_stage_observation (
                block_id INTEGER NOT NULL,
                observed_on TEXT NOT NULL,
                stage TEXT NOT NULL,
                PRIMARY KEY (block_id, observed_on),
                FOREIGN KEY (block_id) REFERENCES block(block_id) ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL("""
            INSERT INTO block_stage_observation (block_id, observed_on, stage)
            SELECT block_id, stage_observed_on, phenological_stage
            FROM block
            WHERE stage_observed_on IS NOT NULL AND phenological_stage IS NOT NULL
        """.trimIndent())
        installBlockStageHistoryTriggers(connection)
    }
}

internal val BLOCK_STAGE_HISTORY_CALLBACK = object : RoomDatabase.Callback() {
    override fun onCreate(connection: SQLiteConnection) {
        installBlockStageHistoryTriggers(connection)
    }
}

private fun installBlockStageHistoryTriggers(connection: SQLiteConnection) {
    connection.execSQL("""
        CREATE TRIGGER block_stage_history_insert AFTER INSERT ON block
        WHEN NEW.stage_observed_on IS NOT NULL AND NEW.phenological_stage IS NOT NULL
        BEGIN
            INSERT INTO block_stage_observation (block_id, observed_on, stage)
            VALUES (NEW.block_id, NEW.stage_observed_on, NEW.phenological_stage);
        END
    """.trimIndent())
    connection.execSQL("""
        CREATE TRIGGER block_stage_history_update
        AFTER UPDATE OF stage_observed_on, phenological_stage ON block
        WHEN NEW.stage_observed_on IS NOT NULL AND NEW.phenological_stage IS NOT NULL
         AND (NEW.stage_observed_on IS NOT OLD.stage_observed_on
              OR NEW.phenological_stage IS NOT OLD.phenological_stage)
        BEGIN
            INSERT INTO block_stage_observation (block_id, observed_on, stage)
            VALUES (NEW.block_id, NEW.stage_observed_on, NEW.phenological_stage)
            ON CONFLICT(block_id, observed_on) DO UPDATE SET stage = excluded.stage;
        END
    """.trimIndent())
}
