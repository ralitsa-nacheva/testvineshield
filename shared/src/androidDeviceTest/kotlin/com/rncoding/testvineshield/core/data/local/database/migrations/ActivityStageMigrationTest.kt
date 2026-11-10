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
import com.rncoding.testvineshield.core.domain.datamodels.ActivityDeletionOutcome
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class ActivityStageMigrationTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val columns=SchemaV2Fixture.columns.mapValues { (table,values) ->
        if(table=="security_settings") values+listOf("inactivity_lock_enabled","inactivity_timeout_millis","device_credential_enabled") else values
    }
    private fun raw(name: String,block: (SQLiteConnection)->Unit) {
        val c=BundledSQLiteDriver().open(context.getDatabasePath(name).absolutePath)
        try {block(c)} finally {c.close()}
    }
    private fun seed(name: String) {
        context.getDatabasePath(name).parentFile?.mkdirs()
        raw(name) {c ->
            SchemaV2Fixture.create(c)
            MIGRATION_2_3.migrate(c)
            MIGRATION_3_4.migrate(c)
            MIGRATION_4_5.migrate(c)
            c.execSQL("INSERT INTO security_settings(user_id,biometric_enabled,app_pin_enabled,inactivity_lock_enabled,inactivity_timeout_millis,device_credential_enabled) VALUES(1,1,0,1,120000,1)")
            c.execSQL("INSERT INTO disease(disease_id,code,name,description,vectors,factors,treatment_suggestion,is_predefined) VALUES(41,'test-disease','Test','','','','',0)")
            c.execSQL("INSERT INTO disease_occurrence(occurrence_id,disease_id,vineyard_id,block_id,observed_at,severity,status,cured_at,created_at,updated_at,notes) VALUES(42,41,10,20,'2026-01-01',1,'active',NULL,1,1,'Test')")
            c.execSQL("INSERT INTO OccurrenceActivityCrossRef(occurrence_id,activity_id,applied_at,created_at,notes) VALUES(42,30,'2026-01-01','2026-01-01','Test')")
            c.execSQL("INSERT INTO activity(activity_id,vineyard_id,activity_type,activity_description,season,phenological_stage,priority,status,planned_at,created_at,updated_at) VALUES(900,10,'Deleted high id','','2026','dormancy','medium','planned','2026-01-01',1,1)")
            c.execSQL("DELETE FROM activity WHERE activity_id=900")
            c.execSQL("PRAGMA user_version=5")
        }
    }
    private fun snapshot(c: SQLiteConnection)=columns.mapValues { (table,fields) ->
        val s=c.prepare("SELECT "+fields.joinToString {"quote(`$it`)"}+" FROM `$table`")
        try {buildList {while(s.step()) add(fields.indices.map {s.getText(it)})}.sortedBy {it.toString()}} finally {s.close()}
    }
    private fun open(name: String)=buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context,name))
    @Test fun v5UpgradePreservesAllRecordsAndKnownStage(): Unit=runBlocking {
        val name="r05a1-preserve-${UUID.randomUUID()}.db";seed(name)
        var before: Map<String,List<List<String>>>?=null;raw(name) {before=snapshot(it)}
        val db=open(name)
        try {
            assertEquals("dormancy",db.activityDao().getActivityById(10,30)?.phenologicalStage?.dbValue)
            assertTrue(db.activityDao().hasDiseaseOccurrenceLinks(30))
            assertTrue(assertNotNull(db.securitySettingsDao().getSettings(1)).inactivityLockEnabled)
            db.close();raw(name) {assertEquals(before,snapshot(it))}
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun unknownStageAndHiddenTreatmentValuesRoundTripAndLinkedDeleteIsRejected(): Unit=runBlocking {
        val name="r05a1-values-${UUID.randomUUID()}.db";seed(name)
        var db=open(name)
        try {
            var repository=ActivityRepositoryImpl(db.activityDao(),db.blockDao())
            val old=assertNotNull(repository.getActivity(10,30))
            val id=assertNotNull(repository.addActivity(old.copy(activityId=0,blockId=null,phenologicalStage=null,
                treatmentProduct="Legacy product",treatmentProductQuantity=0.5,treatmentProductUnits="kg",waterQuantity=10.0,treatmentProductRate=0.125)))
            assertTrue(id>900)
            db.close();db=open(name);repository=ActivityRepositoryImpl(db.activityDao(),db.blockDao())
            val saved=assertNotNull(repository.getActivity(10,id))
            assertNull(saved.phenologicalStage);assertNull(saved.blockId);assertEquals(0.5,saved.treatmentProductQuantity)
            assertTrue(repository.updateActivity(10,saved.copy(notes="Changed only notes")))
            assertEquals("Legacy product",repository.getActivity(10,id)?.treatmentProduct)
            assertNull(repository.observeActivities(10).first().single {it.activityId==id}.phenologicalStage)
            assertEquals(ActivityDeletionOutcome.LINKED_TO_DISEASE,repository.deleteActivity(10,30))
            assertNotNull(repository.getActivity(10,30));assertNull(repository.getActivity(999,id))
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun failedMigrationRollsBackStageConstraintAndCanRetry(): Unit=runBlocking {
        val name="r05a1-rollback-${UUID.randomUUID()}.db";seed(name)
        var before: Map<String,List<List<String>>>?=null;raw(name) {before=snapshot(it)}
        val fail=object: Migration(5,6) {
            override fun migrate(connection: SQLiteConnection) {MIGRATION_5_6.migrate(connection);error("Injected failure")}
        }
        val db=Room.databaseBuilder<VineshieldDatabase>(context,name).setDriver(BundledSQLiteDriver()).addMigrations(fail, MIGRATION_6_7).build()
        try {
            var failed=false;try {db.activityDao().getActivityById(10,30)} catch(_: Exception) {failed=true}
            assertTrue(failed);db.close()
            raw(name) {c ->
                assertEquals(before,snapshot(c))
                val v=c.prepare("PRAGMA user_version");try {assertTrue(v.step());assertEquals(5L,v.getLong(0))} finally {v.close()}
                val info=c.prepare("PRAGMA table_info(activity)")
                try {while(info.step()) if(info.getText(1)=="phenological_stage") assertEquals(1L,info.getLong(3))} finally {info.close()}
            }
            val retry=open(name);try {assertNotNull(retry.activityDao().getActivityById(10,30))} finally {retry.close()}
        } finally {db.close();context.deleteDatabase(name)}
    }
}
