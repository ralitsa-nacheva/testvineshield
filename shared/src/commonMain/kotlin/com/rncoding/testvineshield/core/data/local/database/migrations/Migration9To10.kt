package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Old weather is retained with unknown provenance, never stamped as current-source weather. */
val MIGRATION_9_10 = object : Migration(9,10) {
    override fun migrate(connection: SQLiteConnection) { installWeatherSourceTracking(connection) }
}
val WEATHER_SOURCE_CALLBACK = object : RoomDatabase.Callback() {
    override fun onCreate(connection: SQLiteConnection) { installWeatherSourceTracking(connection) }
}
internal fun installWeatherSourceTracking(connection: SQLiteConnection) {
    connection.execSQL("CREATE TABLE IF NOT EXISTS weather_source (vineyard_id INTEGER NOT NULL PRIMARY KEY, generation TEXT NOT NULL, FOREIGN KEY(vineyard_id) REFERENCES vineyard(vineyard_id) ON DELETE CASCADE)")
    connection.execSQL("CREATE TABLE IF NOT EXISTS weather_history_source (weather_id INTEGER NOT NULL PRIMARY KEY, generation TEXT NOT NULL, fetched_at INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, time_zone TEXT NOT NULL, FOREIGN KEY(weather_id) REFERENCES weather_history(weather_id) ON DELETE CASCADE)")
    connection.execSQL("CREATE TABLE IF NOT EXISTS weather_cache_status (vineyard_id INTEGER NOT NULL, kind TEXT NOT NULL, generation TEXT NOT NULL, token TEXT NOT NULL, state TEXT NOT NULL, attempted_at INTEGER NOT NULL, succeeded_at INTEGER, coverage_start INTEGER, coverage_end INTEGER, hours INTEGER NOT NULL, error TEXT, latitude REAL NOT NULL, longitude REAL NOT NULL, time_zone TEXT NOT NULL, PRIMARY KEY(vineyard_id,kind), FOREIGN KEY(vineyard_id) REFERENCES vineyard(vineyard_id) ON DELETE CASCADE)")
    connection.execSQL("CREATE TABLE IF NOT EXISTS weather_history_archive (archive_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, vineyard_id INTEGER NOT NULL, weather_id INTEGER NOT NULL, archived_at INTEGER NOT NULL, generation TEXT, latitude REAL, longitude REAL, time_zone TEXT, row_json TEXT NOT NULL, row_version INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(vineyard_id) REFERENCES vineyard(vineyard_id) ON DELETE CASCADE)")
    connection.execSQL("CREATE INDEX IF NOT EXISTS index_weather_history_archive_vineyard_id ON weather_history_archive(vineyard_id)")
    connection.execSQL("INSERT OR IGNORE INTO weather_source(vineyard_id,generation) SELECT vineyard_id,lower(hex(randomblob(16))) FROM vineyard")
    connection.execSQL("CREATE TRIGGER IF NOT EXISTS weather_source_insert AFTER INSERT ON vineyard BEGIN INSERT INTO weather_source(vineyard_id,generation) VALUES(NEW.vineyard_id,lower(hex(randomblob(16)))); END")
    connection.execSQL("CREATE TRIGGER IF NOT EXISTS weather_source_change AFTER UPDATE OF latitude,longitude,time_zone,user_id ON vineyard WHEN OLD.latitude IS NOT NEW.latitude OR OLD.longitude IS NOT NEW.longitude OR OLD.time_zone IS NOT NEW.time_zone OR OLD.user_id IS NOT NEW.user_id BEGIN UPDATE weather_source SET generation=lower(hex(randomblob(16))) WHERE vineyard_id=NEW.vineyard_id; DELETE FROM disease_risk_model_state WHERE vineyard_id=NEW.vineyard_id; DELETE FROM disease_risk_evaluation WHERE vineyard_id=NEW.vineyard_id; DELETE FROM weather_calculation WHERE vineyard_id=NEW.vineyard_id; UPDATE disease_alert SET alert_status='invalidated' WHERE vineyard_id=NEW.vineyard_id AND alert_status='active'; END")
}
