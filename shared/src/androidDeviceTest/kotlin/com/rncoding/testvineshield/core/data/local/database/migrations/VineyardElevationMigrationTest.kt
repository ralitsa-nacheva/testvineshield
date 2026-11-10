package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.*
import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class VineyardElevationMigrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun snapshot(connection: SQLiteConnection): Map<String, List<List<String>>> =
        SchemaV2Fixture.columns.mapValues { (table, columns) ->
            val query = "SELECT " + columns.joinToString {
                if (table == "vineyard" && it == "elevation") "quote(CAST(elevation AS REAL))" else if (table == "block" && it in listOf("area", "row_spacing")) "quote(CAST(`$it` AS REAL))" else "quote(`$it`)"
            } + " FROM `$table`"
            val statement = connection.prepare(query)
            try { buildList { while (statement.step()) add(columns.indices.map { statement.getText(it) }) }.sortedBy { it.toString() } }
            finally { statement.close() }
        }
    private fun oldDatabase(name: String): Map<String, List<List<String>>> {
        val path = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }.absolutePath
        val connection = BundledSQLiteDriver().open(path)
        try { SchemaV2Fixture.create(connection); return snapshot(connection) } finally { connection.close() }
    }
    private fun open(name: String) = buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context, name))
    private fun checkRaw(name: String, block: (SQLiteConnection) -> Unit) {
        val connection = BundledSQLiteDriver().open(context.getDatabasePath(name).absolutePath)
        try { block(connection) } finally { connection.close() }
    }
    @Test fun v2UpgradePreservesRowsUnitsRelationshipsAndSequence(): Unit = runBlocking {
        val name = "r03b2-preserve-${UUID.randomUUID()}.db"
        val before = oldDatabase(name)
        val database = open(name)
        try {
            val vineyard = assertNotNull(database.vineyardDao().getVineyard(1, 10))
            assertEquals(200.0, vineyard.elevation)
            assertEquals(-125.0, assertNotNull(database.vineyardDao().getVineyard(1, 11)).elevation)
            assertEquals(0.0, assertNotNull(database.vineyardDao().getVineyard(1, 12)).elevation)
            assertEquals(AreaUnit.HECTARE, vineyard.sizeUnit)
            assertNotNull(database.blockDao().getBlockById(10, 20))
            // Close before reading the complete persisted snapshot through a raw connection.
            database.close()
            checkRaw(name) { connection ->
                assertEquals(before, snapshot(connection))
                val fk = connection.prepare("PRAGMA foreign_key_check")
                try { assertFalse(fk.step()) } finally { fk.close() }
            }
            val reopened = open(name)
            try {
                val id = reopened.vineyardDao().insertVineyard(vineyard.copy(vineyardId = 0, name = "New", elevation = 123.45))
                assertTrue(id > 900, "Never reuse the deleted high-water mark during migration.")
            } finally { reopened.close() }
        } finally { database.close(); context.deleteDatabase(name) }
    }
    @Test fun fractionsAndUnknownElevationSurviveReopen(): Unit = runBlocking {
        val name = "r03b2-values-${UUID.randomUUID()}.db"
        oldDatabase(name)
        var database = open(name)
        try {
            val source = assertNotNull(database.vineyardDao().getVineyard(1, 10))
            val fraction = database.vineyardDao().insertVineyard(source.copy(vineyardId = 0, name = "Fraction", elevation = -12.75))
            val unknown = database.vineyardDao().insertVineyard(source.copy(vineyardId = 0, name = "Unknown", elevation = null))
            database.close(); database = open(name)
            assertEquals(-12.75, assertNotNull(database.vineyardDao().getVineyard(1, fraction)).elevation)
            assertNull(assertNotNull(database.vineyardDao().getVineyard(1, unknown)).elevation)
        } finally { database.close(); context.deleteDatabase(name) }
    }
    @Test fun migrationFailureRollsBackDataAndSchemaAndCanBeRetried(): Unit = runBlocking {
        val name = "r03b2-rollback-${UUID.randomUUID()}.db"
        val before = oldDatabase(name)
        val fail = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                MIGRATION_2_3.migrate(connection)
                error("Injected migration failure")
            }
        }
        val database = Room.databaseBuilder<VineshieldDatabase>(context, name)
            .setDriver(BundledSQLiteDriver()).addMigrations(fail, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build()
        try {
            var failure: Exception? = null
            try { database.vineyardDao().getVineyard(1, 10) } catch (exception: Exception) { failure = exception }
            assertNotNull(failure)
            database.close()
            checkRaw(name) { connection ->
                assertEquals(before, snapshot(connection))
                val version = connection.prepare("PRAGMA user_version")
                try { assertTrue(version.step()); assertEquals(2L, version.getLong(0)) } finally { version.close() }
            }
            val retry = open(name)
            try { assertEquals(200.0, assertNotNull(retry.vineyardDao().getVineyard(1, 10)).elevation) }
            finally { retry.close() }
        } finally { database.close(); context.deleteDatabase(name) }
    }
    @Test fun malformedLegacyElevationIsNotSilentlyConvertedToZero(): Unit = runBlocking {
        val name = "r03b2-malformed-${UUID.randomUUID()}.db"
        oldDatabase(name)
        checkRaw(name) { it.execSQL("UPDATE vineyard SET elevation='invalid text' WHERE vineyard_id=10") }
        val database = open(name)
        try {
            var failure: Exception? = null
            try { database.vineyardDao().getVineyard(1, 10) } catch (exception: Exception) { failure = exception }
            assertNotNull(failure)
            database.close()
            checkRaw(name) { connection ->
                val value = connection.prepare("SELECT elevation,typeof(elevation) FROM vineyard WHERE vineyard_id=10")
                try { assertTrue(value.step()); assertEquals("invalid text", value.getText(0)); assertEquals("text", value.getText(1)) }
                finally { value.close() }
                val version = connection.prepare("PRAGMA user_version")
                try { assertTrue(version.step()); assertEquals(2L, version.getLong(0)) } finally { version.close() }
            }
        } finally { database.close(); context.deleteDatabase(name) }
    }

}
