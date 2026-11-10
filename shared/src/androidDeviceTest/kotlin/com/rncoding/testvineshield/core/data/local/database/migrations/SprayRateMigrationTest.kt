package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.*
import com.rncoding.testvineshield.core.data.repository.SprayRepositoryImpl
import com.rncoding.testvineshield.core.domain.datamodels.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class SprayRateMigrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val columns = SchemaV2Fixture.columns.mapValues { (table, values) ->
        if (table == "security_settings") values + listOf("inactivity_lock_enabled", "inactivity_timeout_millis", "device_credential_enabled") else values
    } + mapOf("spray_application" to listOf("activity_id", "application_date", "local_time", "time_zone", "treated_area", "area_unit", "water_quantity", "water_unit", "notes", "revision", "generation"),
        "spray_product" to listOf("product_line_id", "activity_id", "line_position", "product_name", "quantity", "quantity_unit", "label_reference"))
    private fun raw(name: String, block: (SQLiteConnection) -> Unit) {
        val c = BundledSQLiteDriver().open(context.getDatabasePath(name).absolutePath)
        try { block(c) } finally { c.close() }
    }
    private fun seed(name: String) {
        context.getDatabasePath(name).parentFile?.mkdirs()
        raw(name) { c ->
            SchemaV2Fixture.create(c)
            MIGRATION_2_3.migrate(c); MIGRATION_3_4.migrate(c); MIGRATION_4_5.migrate(c); MIGRATION_5_6.migrate(c); MIGRATION_6_7.migrate(c); MIGRATION_7_8.migrate(c)
            c.execSQL("UPDATE activity SET treatment_product_rate=0.125 WHERE activity_id=30")
            c.execSQL("INSERT INTO spray_application(activity_id,application_date,local_time,time_zone,treated_area,area_unit,water_quantity,water_unit,notes,revision,generation) VALUES(30,'2026-11-10',NULL,'UTC',0.25,'ha',10,'l','Stored notes',3,'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa')")
            c.execSQL("INSERT INTO spray_product(product_line_id,activity_id,line_position,product_name,quantity,quantity_unit,label_reference) VALUES(50,30,0,'A',0.5,'kg','Reference')")
            c.execSQL("INSERT INTO spray_product(product_line_id,activity_id,line_position,product_name,quantity,quantity_unit,label_reference) VALUES(51,30,1,'B',NULL,NULL,NULL)")
            c.execSQL("INSERT INTO spray_product(product_line_id,activity_id,line_position,product_name) VALUES(900,30,2,'Deleted high id')")
            c.execSQL("DELETE FROM spray_product WHERE product_line_id=900")
            c.execSQL("PRAGMA user_version=8")
        }
    }
    private fun snapshot(c: SQLiteConnection) = columns.mapValues { (table, fields) ->
        val s = c.prepare("SELECT " + fields.joinToString { "quote(`$it`)" } + " FROM `$table`")
        try { buildList { while (s.step()) add(fields.indices.map { s.getText(it) }) }.sortedBy { it.toString() } } finally { s.close() }
    }
    private fun open(name: String) = buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context, name))
    @Test fun upgradePreservesAllTwentyTwoTablesAndLeavesRatesUnknown(): Unit = runBlocking {
        val name = "r05a4-preserve-${UUID.randomUUID()}.db"; seed(name)
        var before: Map<String, List<List<String>>>? = null; raw(name) { before = snapshot(it) }
        val db = open(name)
        try {
            val record = assertNotNull(SprayRepositoryImpl(db.sprayDao()).get(10, 30))
            assertEquals(3L, record.revision); assertEquals("a".repeat(32), record.generation)
            assertTrue(record.products.all { it.rate == null })
            assertEquals(0.125, db.activityDao().getActivityById(10, 30)?.treatmentProductRate)
            db.close(); raw(name) { assertEquals(before, snapshot(it)) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun rateBasisRoundTripClearAndPartialStorageIsNotSilentlyUnknown(): Unit = runBlocking {
        val name = "r05a4-values-${UUID.randomUUID()}.db"; seed(name)
        var db = open(name)
        try {
            var repo = SprayRepositoryImpl(db.sprayDao())
            val old = assertNotNull(repo.get(10, 30))
            val areaRate = SprayRate(2.0, SprayQuantityUnit.KILOGRAM, 1.0, SprayRateBasis.HECTARE)
            val carrierRate = SprayRate(100.0, SprayQuantityUnit.MILLILITRE, 100.0, SprayRateBasis.LITRE)
            val edited = old.copy(products = old.products.mapIndexed { index, line -> if (index == 0) line.copy(rate = areaRate) else line } + SprayProductLine(productName = "C", rate = carrierRate))
            assertEquals(SprayWriteOutcome.SAVED, repo.save(10, edited)); assertEquals(SprayWriteOutcome.CONFLICT, repo.save(10, old))
            db.close(); db = open(name); repo = SprayRepositoryImpl(db.sprayDao())
            val saved = assertNotNull(repo.get(10, 30))
            assertEquals(areaRate, saved.products[0].rate); assertNull(saved.products[1].rate); assertEquals(carrierRate, saved.products[2].rate)
            assertTrue(saved.products[2].lineId > 900); assertEquals(0.5, saved.products[0].quantity)
            assertEquals(SprayWriteOutcome.SAVED, repo.save(10, saved.copy(products = saved.products.map { it.copy(rate = null) })))
            assertTrue(assertNotNull(repo.get(10, 30)).products.all { it.rate == null })
            db.close()
            raw(name) { it.execSQL("UPDATE spray_product SET rate_quantity=2, rate_product_unit='kg', rate_basis_quantity=1, rate_basis_unit=NULL WHERE product_line_id=50") }
            db = open(name); repo = SprayRepositoryImpl(db.sprayDao())
            assertFailsWith<IllegalStateException> { repo.get(10, 30) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun failedMigrationRestoresOriginalColumnsAndCanRetry(): Unit = runBlocking {
        val name = "r05a4-rollback-${UUID.randomUUID()}.db"; seed(name)
        var before: Map<String, List<List<String>>>? = null; raw(name) { before = snapshot(it) }
        val fail = object : Migration(8, 9) { override fun migrate(connection: SQLiteConnection) { MIGRATION_8_9.migrate(connection); error("Injected failure") } }
        val db = Room.databaseBuilder<VineshieldDatabase>(context, name).setDriver(BundledSQLiteDriver()).addMigrations(fail).build()
        try {
            var failed = false; try { db.sprayDao().get(10, 30) } catch (_: Exception) { failed = true }
            assertTrue(failed); db.close()
            raw(name) { c ->
                assertEquals(before, snapshot(c))
                val s = c.prepare("PRAGMA table_info(spray_product)")
                try { while (s.step()) assertFalse(s.getText(1).startsWith("rate_")) } finally { s.close() }
            }
            val retry = open(name); try { assertNotNull(SprayRepositoryImpl(retry.sprayDao()).get(10, 30)) } finally { retry.close() }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
