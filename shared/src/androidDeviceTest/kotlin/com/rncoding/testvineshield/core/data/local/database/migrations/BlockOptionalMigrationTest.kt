package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class BlockOptionalMigrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val columns = SchemaV2Fixture.columns.mapValues { (table, cols) ->
        if (table == "security_settings") cols + listOf("inactivity_lock_enabled", "inactivity_timeout_millis", "device_credential_enabled") else cols
    }
    private fun raw(name: String, block: (SQLiteConnection) -> Unit) {
        val connection = BundledSQLiteDriver().open(context.getDatabasePath(name).absolutePath)
        try { block(connection) } finally { connection.close() }
    }
    private fun seed(name: String) {
        context.getDatabasePath(name).parentFile?.mkdirs()
        raw(name) { c ->
            SchemaV2Fixture.create(c); MIGRATION_2_3.migrate(c); MIGRATION_3_4.migrate(c)
            c.execSQL("INSERT INTO security_settings(user_id,biometric_enabled,app_pin_enabled,inactivity_lock_enabled,inactivity_timeout_millis,device_credential_enabled) VALUES(1,1,0,1,120000,1)")
            c.execSQL("INSERT INTO block SELECT 21,vineyard_id,'Legacy zero',0,vine_variety,color,root_stock,0,0,'1970-01-01',phenological_stage,area_unit,row_spacing_unit FROM block WHERE block_id=20")
            c.execSQL("INSERT INTO block SELECT 900,vineyard_id,name,area,vine_variety,color,root_stock,row_count,row_spacing,planted_at,phenological_stage,area_unit,row_spacing_unit FROM block WHERE block_id=20")
            c.execSQL("DELETE FROM block WHERE block_id=900")
            c.execSQL("PRAGMA user_version=4")
        }
    }
    private fun snapshot(c: SQLiteConnection): Map<String,List<List<String>>> = columns.mapValues { (table, fields) ->
        val sql = "SELECT " + fields.joinToString { if (table == "block" && it in listOf("area", "row_spacing")) "quote(CAST(`$it` AS REAL))" else "quote(`$it`)" } + " FROM `$table`"
        val s = c.prepare(sql)
        try { buildList { while(s.step()) add(fields.indices.map { s.getText(it) }) }.sortedBy { it.toString() } }
        finally { s.close() }
    }
    private fun open(name: String) = buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context, name))
    @Test fun v4UpgradePreservesEveryOldValueChildrenAndLockPolicy(): Unit = runBlocking {
        val name = "r03d-preserve-${UUID.randomUUID()}.db"; seed(name)
        var before: Map<String,List<List<String>>>? = null
        raw(name) { before = snapshot(it) }
        val db = open(name)
        try {
            val block = assertNotNull(db.blockDao().getBlockById(10,20))
            assertEquals(7.0, block.area); assertEquals(2.0, block.rowSpacing); assertEquals(5, block.rowCount)
            assertNotNull(block.plantedAt); assertNull(block.phenologicalStage)
            val zero = assertNotNull(db.blockDao().getBlockById(10,21))
            assertEquals(0.0,zero.area); assertEquals(0.0,zero.rowSpacing); assertEquals(0,zero.rowCount)
            assertEquals("1970-01-01",zero.plantedAt.toString())
            val settings = assertNotNull(db.securitySettingsDao().getSettings(1))
            assertTrue(settings.inactivityLockEnabled); assertTrue(settings.biometricEnabled); assertEquals(120000L, settings.inactivityTimeoutMillis)
            db.close()
            raw(name) { c ->
                assertEquals(before, snapshot(c))
                val fk = c.prepare("PRAGMA foreign_key_check")
                try { assertFalse(fk.step()) } finally { fk.close() }
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun fractionalAndUnknownFieldsRoundTripThroughDaoAndSummary(): Unit = runBlocking {
        val name = "r03d-values-${UUID.randomUUID()}.db"; seed(name)
        var db = open(name)
        try {
            val old = assertNotNull(db.blockDao().getBlockById(10,20))
            val id = db.blockDao().insertBlock(old.copy(blockId=0, name="Fraction", area=1.25, rowSpacing=2.45, rowCount=null, plantedAt=null))
            assertTrue(id > 900)
            db.close(); db = open(name)
            val saved = assertNotNull(db.blockDao().getBlockById(10,id))
            assertEquals(1.25,saved.area); assertEquals(2.45,saved.rowSpacing); assertNull(saved.rowCount); assertNull(saved.plantedAt)
            assertEquals(old.areaUnit,saved.areaUnit); assertEquals(old.rowSpacingUnit,saved.rowSpacingUnit)
            assertEquals(1.25, db.blockDao().observeBlockSummariesForVineyard(10).first().single { it.blockId == id }.area)
            val mapper = com.rncoding.testvineshield.core.data.local.mappers.BlockMapper()
            val repository = com.rncoding.testvineshield.core.data.repository.BlockRepositoryImpl(db.blockDao(),mapper)
            assertTrue(repository.updateBlock(mapper.toDomain(saved).copy(area=null,rowSpacing=null)))
            db.close(); db = open(name)
            assertNull(assertNotNull(db.blockDao().getBlockById(10,id)).area)
            assertNull(db.blockDao().observeBlockSummariesForVineyard(10).first().single { it.blockId == id }.area)
            assertNull(db.blockDao().getBlockById(999,id))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun failureAfterColumnChangesRollsBackAndCanRetry(): Unit = runBlocking {
        val name = "r03d-rollback-${UUID.randomUUID()}.db"; seed(name)
        var before: Map<String,List<List<String>>>? = null; raw(name) { before=snapshot(it) }
        val fail = object : Migration(4,5) {
            override fun migrate(connection: SQLiteConnection) { MIGRATION_4_5.migrate(connection); error("Injected failure") }
        }
        val db = Room.databaseBuilder<VineshieldDatabase>(context,name).setDriver(BundledSQLiteDriver()).addMigrations(fail, MIGRATION_5_6, MIGRATION_6_7).build()
        try {
            var failed=false; try { db.blockDao().getBlockById(10,20) } catch (_: Exception) { failed=true }
            assertTrue(failed); db.close()
            raw(name) { c ->
                assertEquals(before,snapshot(c))
                val v=c.prepare("PRAGMA user_version");try { assertTrue(v.step());assertEquals(4L,v.getLong(0)) } finally { v.close() }
            }
            val retry=open(name);try { assertEquals(7.0,assertNotNull(retry.blockDao().getBlockById(10,20)).area) } finally { retry.close() }
        } finally { db.close();context.deleteDatabase(name) }
    }
    @Test fun malformedLegacyNumberIsNotCastToZero(): Unit = runBlocking {
        val name = "r03d-corrupt-${UUID.randomUUID()}.db";seed(name)
        raw(name) { it.execSQL("UPDATE block SET row_spacing='bad value' WHERE block_id=20") }
        val db=open(name)
        try {
            var failed=false;try { db.blockDao().getBlockById(10,20) } catch (_: Exception) { failed=true }
            assertTrue(failed);db.close()
            raw(name) { c ->
                val s=c.prepare("SELECT row_spacing,typeof(row_spacing) FROM block WHERE block_id=20")
                try { assertTrue(s.step());assertEquals("bad value",s.getText(0));assertEquals("text",s.getText(1)) } finally { s.close() }
            }
        } finally { db.close();context.deleteDatabase(name) }
    }
}
