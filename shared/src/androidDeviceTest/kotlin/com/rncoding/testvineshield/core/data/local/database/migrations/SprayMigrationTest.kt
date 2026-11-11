package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.*
import com.rncoding.testvineshield.core.data.repository.ActivityRepositoryImpl
import com.rncoding.testvineshield.core.data.repository.SprayRepositoryImpl
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class SprayMigrationTest {
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
            MIGRATION_2_3.migrate(c); MIGRATION_3_4.migrate(c); MIGRATION_4_5.migrate(c); MIGRATION_5_6.migrate(c); MIGRATION_6_7.migrate(c)
            c.execSQL("INSERT INTO security_settings(user_id,biometric_enabled,app_pin_enabled,inactivity_lock_enabled,inactivity_timeout_millis,device_credential_enabled) VALUES(1,1,0,1,120000,1)")
            c.execSQL("UPDATE activity SET treatment_product='Legacy product',treatment_product_quantity=0.5,treatment_product_units='kg',water_quantity=10,treatment_product_rate=0.125 WHERE activity_id=30")
            c.execSQL("INSERT INTO activity(activity_id,vineyard_id,activity_type,activity_description,season,priority,status,planned_at,created_at,updated_at) VALUES(31,10,'Other task','','2026','medium','planned','2026-11-10',1,1)")
            c.execSQL("INSERT INTO disease(disease_id,code,name,description,vectors,factors,treatment_suggestion,is_predefined) VALUES(41,'test-disease','Test','','','','',0)")
            c.execSQL("INSERT INTO disease_occurrence(occurrence_id,disease_id,vineyard_id,block_id,observed_at,severity,status,cured_at,created_at,updated_at,notes) VALUES(42,41,10,20,'2026-01-01',1,'active',NULL,1,1,'Test')")
            c.execSQL("INSERT INTO OccurrenceActivityCrossRef(occurrence_id,activity_id,applied_at,created_at,notes) VALUES(42,30,'2026-01-01','2026-01-01','Test')")
            c.execSQL("PRAGMA user_version=7")
        }
    }
    private fun snapshot(c: SQLiteConnection) = columns.mapValues { (table, fields) ->
        val s = c.prepare("SELECT " + fields.joinToString { "quote(`$it`)" } + " FROM `$table`")
        try { buildList { while (s.step()) add(fields.indices.map { s.getText(it) }) }.sortedBy { it.toString() } } finally { s.close() }
    }
    private fun open(name: String) = buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context, name))
    private fun record(activityId: Long = 30) = SprayRecord(activityId, LocalDate(2026, 11, 10), "15:30", "UTC", 0.25, AreaUnit.HECTARE,
        10.0, SprayQuantityUnit.US_GALLON, "Recorded facts", products = listOf(SprayProductLine(productName = "A", quantity = 0.5, unit = SprayQuantityUnit.KILOGRAM), SprayProductLine(productName = "B")))

    @Test fun v7UpgradePreservesAllExistingRowsAndDoesNotInventSprayRecords(): Unit = runBlocking {
        val name = "r05a3-preserve-${UUID.randomUUID()}.db"; seed(name)
        var before: Map<String, List<List<String>>>? = null; raw(name) { before = snapshot(it) }
        val db = open(name)
        try {
            assertNull(db.sprayDao().get(10, 30)); assertTrue(db.sprayDao().lines(30).isEmpty())
            assertEquals("Legacy product", db.activityDao().getActivityById(10, 30)?.treatmentProduct)
            db.close(); raw(name) { assertEquals(before, snapshot(it)) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun productsRoundTripReorderAndRejectForeignIdsAndStaleRevisions(): Unit = runBlocking {
        val name = "r05a3-values-${UUID.randomUUID()}.db"; seed(name)
        var db = open(name)
        try {
            var repo = SprayRepositoryImpl(db.sprayDao())
            val activityBefore = db.activityDao().getActivityById(10, 30)
            assertEquals(SprayWriteOutcome.SAVED, repo.save(10, record()))
            val first = assertNotNull(repo.get(10, 30)); assertEquals(1L, first.revision)
            assertTrue(first.products.all { it.lineId > 0 }); assertNull(first.products[1].quantity)
            assertEquals(SprayWriteOutcome.SAVED, repo.save(10, first.copy(products = first.products.reversed())))
            assertEquals(SprayWriteOutcome.CONFLICT, repo.save(10, first))
            assertEquals(SprayWriteOutcome.SAVED, repo.save(10, record(31)))
            val foreign = assertNotNull(repo.get(10, 31)).products.first()
            val latest = assertNotNull(repo.get(10, 30))
            assertEquals(SprayWriteOutcome.INVALID_LINE_ID, repo.save(10, latest.copy(products = listOf(foreign))))
            assertEquals(SprayWriteOutcome.ACTIVITY_NOT_FOUND, repo.save(999, latest))
            assertNull(repo.get(999, 30))
            db.close(); db = open(name); repo = SprayRepositoryImpl(db.sprayDao())
            val reopened = assertNotNull(repo.get(10, 30))
            assertEquals(first.products.reversed(), reopened.products); assertEquals(2L, reopened.revision)
            assertEquals(activityBefore, db.activityDao().getActivityById(10, 30))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun deletionRetainsActivityLinksAndOldGenerationCannotDeleteRecreatedRecord(): Unit = runBlocking {
        val name = "r05a3-delete-${UUID.randomUUID()}.db"; seed(name)
        val db = open(name)
        try {
            val repo = SprayRepositoryImpl(db.sprayDao())
            assertEquals(SprayWriteOutcome.SAVED, repo.save(10, record()))
            val old = assertNotNull(repo.get(10, 30)); val generation = assertNotNull(old.generation)
            val activities = ActivityRepositoryImpl(db.activityDao(), db.blockDao())
            assertEquals(ActivityDeletionOutcome.LINKED_TO_DISEASE, activities.deleteActivity(10, 30))
            assertEquals(SprayWriteOutcome.SAVED, repo.delete(10, 30, old.revision, generation))
            assertNotNull(db.activityDao().getActivityById(10, 30)); assertTrue(db.activityDao().hasDiseaseOccurrenceLinks(30))
            assertEquals(SprayWriteOutcome.SAVED, repo.save(10, record()))
            val fresh = assertNotNull(repo.get(10, 30)); assertNotEquals(generation, fresh.generation)
            assertEquals(SprayWriteOutcome.CONFLICT, repo.delete(10, 30, old.revision, generation))
            assertEquals(SprayWriteOutcome.CONFLICT, repo.save(10, old))
            assertEquals(SprayWriteOutcome.SAVED, repo.save(10, record(31)))
            assertEquals(ActivityDeletionOutcome.DELETED, activities.deleteActivity(10, 31))
            assertNull(repo.get(10, 31)); assertTrue(db.sprayDao().lines(31).isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun failedLineInsertionRollsBackHeaderAndEntireProductList(): Unit = runBlocking {
        val name = "r05a3-atomic-${UUID.randomUUID()}.db"; seed(name)
        var db = open(name)
        try {
            var repo = SprayRepositoryImpl(db.sprayDao()); assertEquals(SprayWriteOutcome.SAVED, repo.save(10, record()))
            val before = assertNotNull(repo.get(10, 30)); db.close()
            raw(name) { it.execSQL("CREATE TRIGGER test_abort_spray BEFORE INSERT ON spray_product WHEN NEW.product_name='Injected failure' BEGIN SELECT RAISE(ABORT,'Injected failure'); END") }
            db = open(name); repo = SprayRepositoryImpl(db.sprayDao())
            assertFailsWith<Exception> { repo.save(10, before.copy(notes = "Must roll back", products = before.products.mapIndexed { index, line -> if (index == 1) line.copy(productName = "Injected failure") else line })) }
            assertEquals(before, repo.get(10, 30))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun failedMigrationRollsBackNewTablesAndCanRetry(): Unit = runBlocking {
        val name = "r05a3-rollback-${UUID.randomUUID()}.db"; seed(name)
        var before: Map<String, List<List<String>>>? = null; raw(name) { before = snapshot(it) }
        val fail = object : Migration(7, 8) {
            override fun migrate(connection: SQLiteConnection) { MIGRATION_7_8.migrate(connection); error("Injected migration failure") }
        }
        val db = Room.databaseBuilder<VineshieldDatabase>(context, name).setDriver(BundledSQLiteDriver()).addMigrations(fail, MIGRATION_8_9, MIGRATION_9_10).build()
        try {
            var failed = false; try { db.sprayDao().get(10, 30) } catch (_: Exception) { failed = true }
            assertTrue(failed); db.close()
            raw(name) { c ->
                assertEquals(before, snapshot(c))
                val s = c.prepare("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('spray_application','spray_product')")
                try { assertTrue(s.step()); assertEquals(0L, s.getLong(0)) } finally { s.close() }
            }
            val retry = open(name); try { assertNull(retry.sprayDao().get(10, 30)) } finally { retry.close() }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
