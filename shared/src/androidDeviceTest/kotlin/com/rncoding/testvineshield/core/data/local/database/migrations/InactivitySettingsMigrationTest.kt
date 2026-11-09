package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.*
import com.rncoding.testvineshield.core.data.local.database.entities.SecuritySettingsEntity
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class InactivitySettingsMigrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    /** Actual v1/v2 SQL plus production 2->3, not an invented compiler-generated v3 identity. */
    private fun seed(name: String) {
        val path = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }.absolutePath
        val connection = BundledSQLiteDriver().open(path)
        try {
            SchemaV2Fixture.create(connection)
            MIGRATION_2_3.migrate(connection)
            connection.execSQL("INSERT INTO security_settings(user_id,biometric_enabled,app_pin_enabled) VALUES(1,1,1)")
            connection.execSQL("PRAGMA user_version=3")
        } finally { connection.close() }
    }
    @Test fun upgradeDisablesLegacyMethodsWithoutRemovingAccountOrVineyard(): Unit = runBlocking {
        val name = "r03c-migrate-${UUID.randomUUID()}.db"; seed(name)
        val db = buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context, name))
        try {
            val settings = assertNotNull(db.securitySettingsDao().getSettings(1))
            assertFalse(settings.inactivityLockEnabled); assertFalse(settings.biometricEnabled)
            assertFalse(settings.appPinEnabled); assertFalse(settings.deviceCredentialEnabled)
            assertEquals(900000L, settings.inactivityTimeoutMillis)
            assertEquals(200.0, assertNotNull(db.vineyardDao().getVineyard(1, 10)).elevation)
            assertNotNull(db.blockDao().getBlockById(10, 20))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun selectedPolicySurvivesReopen(): Unit = runBlocking {
        val name = "r03c-policy-${UUID.randomUUID()}.db"; seed(name)
        var db = buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context, name))
        try {
            db.securitySettingsDao().upsertSettings(SecuritySettingsEntity(1, true, false, true, 120000, true))
            db.close(); db = buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context, name))
            val saved = assertNotNull(db.securitySettingsDao().getSettings(1))
            assertTrue(saved.inactivityLockEnabled); assertTrue(saved.biometricEnabled); assertTrue(saved.deviceCredentialEnabled)
            assertEquals(120000L, saved.inactivityTimeoutMillis)
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun failedMigrationRollsBackPolicyResetAndColumns(): Unit = runBlocking {
        val name = "r03c-rollback-${UUID.randomUUID()}.db"; seed(name)
        val failing = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                MIGRATION_3_4.migrate(connection); error("Injected failure")
            }
        }
        val db = Room.databaseBuilder<VineshieldDatabase>(context, name)
            .setDriver(BundledSQLiteDriver()).addMigrations(failing, MIGRATION_4_5, MIGRATION_5_6).build()
        try {
            var failed = false
            try { db.securitySettingsDao().getSettings(1) } catch (_: Exception) { failed = true }
            assertTrue(failed); db.close()
            val raw = BundledSQLiteDriver().open(context.getDatabasePath(name).absolutePath)
            try {
                val row = raw.prepare("SELECT biometric_enabled,app_pin_enabled FROM security_settings WHERE user_id=1")
                try { assertTrue(row.step()); assertEquals(1L,row.getLong(0)); assertEquals(1L,row.getLong(1)) } finally { row.close() }
                val version = raw.prepare("PRAGMA user_version")
                try { assertTrue(version.step()); assertEquals(3L, version.getLong(0)) } finally { version.close() }
                val columns = raw.prepare("PRAGMA table_info(security_settings)")
                try { while(columns.step()) assertNotEquals("inactivity_lock_enabled", columns.getText(1)) } finally { columns.close() }
            } finally { raw.close() }
            val retry = buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context, name))
            try { assertFalse(assertNotNull(retry.securitySettingsDao().getSettings(1)).inactivityLockEnabled) } finally { retry.close() }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
