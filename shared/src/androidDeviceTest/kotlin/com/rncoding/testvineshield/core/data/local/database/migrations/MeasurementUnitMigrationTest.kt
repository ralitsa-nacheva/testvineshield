package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.buildVineshieldDatabase
import com.rncoding.testvineshield.core.domain.datamodels.units.*
import com.rncoding.testvineshield.core.data.local.mappers.*
import com.rncoding.testvineshield.core.domain.repository.*
import com.rncoding.testvineshield.di.commonModule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.util.UUID
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class MeasurementUnitMigrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun snapshot(connection: SQLiteConnection): Map<String, List<List<String>>> =
        SchemaV1Fixture.columns.mapValues { (table, columns) ->
            val query = "SELECT " + columns.joinToString { if (table == "vineyard" && it == "elevation") "quote(CAST(elevation AS REAL))" else if (table == "block" && it in listOf("area", "row_spacing")) "quote(CAST(`$it` AS REAL))" else "quote(`$it`)" } + " FROM `$table`"
            val statement = connection.prepare(query)
            try {
                buildList {
                    while (statement.step()) add(columns.indices.map { statement.getText(it) })
                }.sortedBy { it.toString() }
            } finally { statement.close() }
        }

    @Test fun populatedV1MigratesThroughProductionBuilderAndPreservesEveryOldColumn(): Unit = runBlocking {
        val name = "r03b1-migration-${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }.absolutePath
        var database: VineshieldDatabase? = null
        try {
            val before: Map<String, List<List<String>>>
            val old = BundledSQLiteDriver().open(path)
            try {
                old.execSQL("PRAGMA foreign_keys = ON")
                SchemaV1Fixture.statements.forEach { old.execSQL(it) }
                before = snapshot(old)
            } finally { old.close() }
            val migrated = buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context, name))
            database = migrated
            // First query opens Room, runs 1->2 and validates the actual compiler-generated target schema.
            val vineyard = assertNotNull(migrated.vineyardDao().getVineyard(1, 10))
            val block = assertNotNull(migrated.blockDao().getBlockById(10, 20))
            assertNull(vineyard.sizeUnit); assertNull(block.areaUnit); assertNull(block.rowSpacingUnit)
            assertEquals(1.25, vineyard.size); assertEquals(7.0, block.area); assertNull(block.phenologicalStage)
            migrated.close(); database = null
            val upgraded = BundledSQLiteDriver().open(path)
            try {
                assertEquals(before, snapshot(upgraded))
                val version = upgraded.prepare("PRAGMA user_version")
                try { assertTrue(version.step()); assertEquals(10L, version.getLong(0)) } finally { version.close() }
                val foreignKeys = upgraded.prepare("PRAGMA foreign_key_check")
                try { assertFalse(foreignKeys.step()) } finally { foreignKeys.close() }
            } finally { upgraded.close() }
        } finally { database?.close(); context.deleteDatabase(name) }
    }

    @Test fun explicitUnitsRoundTripThroughRepositoriesAndSummaryAfterReopen(): Unit = runBlocking {
        val name = "r03b1-roundtrip-${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }.absolutePath
        var database: VineshieldDatabase? = null
        try {
            val old = BundledSQLiteDriver().open(path)
            try { SchemaV1Fixture.statements.forEach { old.execSQL(it) } } finally { old.close() }
            val migrated = buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context, name))
            database = migrated
            val app = koinApplication { modules(commonModule, module { single<VineshieldDatabase> { migrated } }) }
            try {
                val vineyardRepository = app.koin.get<VineyardRepository>()
                val blockRepository = app.koin.get<BlockRepository>()
                val vineyard = VineyardMapper().entityToDomain(assertNotNull(migrated.vineyardDao().getVineyard(1, 10)))
                assertIs<com.rncoding.testvineshield.core.domain.error.Result.Success<*>>(
                    vineyardRepository.updateVineyard(vineyard.copy(sizeUnit = AreaUnit.HECTARE)))
                val block = assertNotNull(blockRepository.getBlock(10, 20))
                assertTrue(blockRepository.updateBlock(block.copy(areaUnit = AreaUnit.SQUARE_METRE, rowSpacingUnit = LengthUnit.METRE)))
                assertEquals(AreaUnit.SQUARE_METRE, vineyardRepository.observeBlockSummaries(10).first().single().areaUnit)
            } finally { app.close() }
            migrated.close(); database = null
            val reopened = buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context, name))
            database = reopened
            assertEquals(AreaUnit.HECTARE, assertNotNull(reopened.vineyardDao().getVineyard(1, 10)).sizeUnit)
            val block = assertNotNull(reopened.blockDao().getBlockById(10, 20))
            assertEquals(AreaUnit.SQUARE_METRE, block.areaUnit); assertEquals(LengthUnit.METRE, block.rowSpacingUnit)
            assertEquals(7.0, block.area); assertEquals(2.0, block.rowSpacing)
        } finally { database?.close(); context.deleteDatabase(name) }
    }
}
