package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.*
import com.rncoding.testvineshield.core.data.local.mappers.HarvestMapper
import com.rncoding.testvineshield.core.data.repository.HarvestRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class HarvestBrixMigrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val columns = SchemaV2Fixture.columns.mapValues { (table, values) ->
        if (table == "security_settings") values + listOf("inactivity_lock_enabled", "inactivity_timeout_millis", "device_credential_enabled") else values
    }
    private fun raw(name: String, block: (SQLiteConnection) -> Unit) {
        val c = BundledSQLiteDriver().open(context.getDatabasePath(name).absolutePath)
        try { block(c) } finally { c.close() }
    }
    private fun seed(name: String) {
        context.getDatabasePath(name).parentFile?.mkdirs()
        raw(name) { c ->
            SchemaV2Fixture.create(c)
            MIGRATION_2_3.migrate(c)
            MIGRATION_3_4.migrate(c)
            MIGRATION_4_5.migrate(c)
            MIGRATION_5_6.migrate(c)
            c.execSQL("INSERT INTO security_settings(user_id,biometric_enabled,app_pin_enabled,inactivity_lock_enabled,inactivity_timeout_millis,device_credential_enabled) VALUES(1,1,0,1,120000,1)")
            c.execSQL("INSERT INTO harvest(harvest_id,block_id,season,harvest_date,units,quantity,brix_sugar_content) VALUES(41,20,'2026/2027','2026-11-01','baskets',12.5,0)")
            c.execSQL("INSERT INTO harvest(harvest_id,block_id,season,harvest_date,units,quantity,brix_sugar_content) VALUES(900,20,'2026','2026-11-01','kg',1,22.5)")
            c.execSQL("DELETE FROM harvest WHERE harvest_id=900")
            c.execSQL("PRAGMA user_version=6")
        }
    }
    private fun snapshot(c: SQLiteConnection) = columns.mapValues { (table, fields) ->
        val s = c.prepare("SELECT " + fields.joinToString { "quote(`$it`)" } + " FROM `$table`")
        try {
            buildList { while (s.step()) add(fields.indices.map { s.getText(it) }) }.sortedBy { it.toString() }
        } finally { s.close() }
    }
    private fun open(name: String) = buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context, name))

    @Test fun v6UpgradePreservesEveryTableAndZeroBrix(): Unit = runBlocking {
        val name = "r05c1-preserve-${UUID.randomUUID()}.db"; seed(name)
        var before: Map<String, List<List<String>>>? = null
        raw(name) { before = snapshot(it) }
        val db = open(name)
        try {
            assertEquals(0.0, db.harvestDao().getHarvestForBlock(20, 41)?.brixSugarContent)
            assertEquals("baskets", db.harvestDao().getHarvestForBlock(20, 41)?.units)
            assertTrue(assertNotNull(db.securitySettingsDao().getSettings(1)).inactivityLockEnabled)
            db.close(); raw(name) { assertEquals(before, snapshot(it)) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun nullableBrixRoundTripsAcrossReopenUpdateFlowAndScopedDeletion(): Unit = runBlocking {
        val name = "r05c1-roundtrip-${UUID.randomUUID()}.db"; seed(name)
        var db = open(name)
        try {
            var repository = HarvestRepositoryImpl(db.harvestDao(), HarvestMapper())
            val old = assertNotNull(repository.getHarvest(20, 41))
            val id = assertNotNull(repository.createHarvest(old.copy(harvestId = 0, units = "kg", brixSugarContent = null)))
            assertTrue(id > 900)
            db.close(); db = open(name); repository = HarvestRepositoryImpl(db.harvestDao(), HarvestMapper())
            val saved = assertNotNull(repository.getHarvest(20, id))
            assertEquals(old.copy(harvestId = id, units = "kg", brixSugarContent = null), saved)
            assertTrue(repository.updateHarvest(saved.copy(brixSugarContent = 22.5)))
            assertEquals(22.5, repository.getHarvest(20, id)?.brixSugarContent)
            assertTrue(repository.updateHarvest(saved.copy(quantity = 25.5)))
            assertNull(repository.observeHarvestsForBlock(20).first().single { it.harvestId == id }.brixSugarContent)
            assertNull(repository.getHarvest(999, id))
            assertFalse(repository.updateHarvest(saved.copy(blockId = 999)))
            assertFalse(repository.deleteHarvest(999, id))
            assertNotNull(repository.getHarvest(20, id))
            assertTrue(repository.deleteHarvest(20, id))
            assertNull(repository.getHarvest(20, id))
            assertNotNull(repository.getHarvest(20, 41))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun failedMigrationRollsBackOriginalConstraintRowsAndVersionThenRetries(): Unit = runBlocking {
        val name = "r05c1-rollback-${UUID.randomUUID()}.db"; seed(name)
        var before: Map<String, List<List<String>>>? = null
        raw(name) { before = snapshot(it) }
        val fail = object : Migration(6, 7) {
            override fun migrate(connection: SQLiteConnection) { MIGRATION_6_7.migrate(connection); error("Injected failure") }
        }
        val db = Room.databaseBuilder<VineshieldDatabase>(context, name).setDriver(BundledSQLiteDriver()).addMigrations(fail, MIGRATION_7_8).build()
        try {
            var failed = false
            try { db.harvestDao().getHarvestForBlock(20, 41) } catch (_: Exception) { failed = true }
            assertTrue(failed); db.close()
            raw(name) { c ->
                assertEquals(before, snapshot(c))
                val v = c.prepare("PRAGMA user_version")
                try { assertTrue(v.step()); assertEquals(6L, v.getLong(0)) } finally { v.close() }
                val info = c.prepare("PRAGMA table_info(harvest)")
                try { while (info.step()) if (info.getText(1) == "brix_sugar_content") assertEquals(1L, info.getLong(3)) } finally { info.close() }
            }
            val retry = open(name)
            try { assertNotNull(retry.harvestDao().getHarvestForBlock(20, 41)) } finally { retry.close() }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
