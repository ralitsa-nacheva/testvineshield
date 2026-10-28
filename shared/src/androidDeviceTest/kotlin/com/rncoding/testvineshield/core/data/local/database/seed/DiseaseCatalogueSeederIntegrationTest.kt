package com.rncoding.testvineshield.core.data.local.database.seed

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DiseaseCatalogueSeederIntegrationTest {
    private lateinit var database: VineshieldDatabase
    private lateinit var dao: DiseaseDao
    private lateinit var seeder: DiseaseCatalogueSeeder

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder<VineshieldDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.diseaseDao()
        seeder = DiseaseCatalogueSeeder(database)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun emptyDatabaseReceivesCompleteCatalogue(): Unit = runBlocking {
        seeder.seed()
        val rows = dao.observeDiseases().first()
        assertEquals(3, rows.size)
        assertEquals(
            setOf("grape_powdery_mildew", "grape_downy_mildew", "grape_botrytis"),
            rows.map { it.code }.toSet()
        )
        assertTrue(rows.all { it.isPredefined && it.diseaseId > 0L })
        for (expected in PredefinedDiseaseCatalogue.diseases) {
            val actual = assertNotNull(dao.getDiseaseByCode(requireNotNull(expected.code)))
            assertEquals(expected.copy(diseaseId = actual.diseaseId), actual)
        }
    }

    @Test
    fun repeatedSeedingPreservesIdsAndContent(): Unit = runBlocking {
        seeder.seed()
        val before = dao.observeDiseases().first()
        seeder.seed()
        assertEquals(before, dao.observeDiseases().first())
    }

    @Test
    fun partialCatalogueIsCompletedWithoutReplacingExistingRows(): Unit = runBlocking {
        val original = PredefinedDiseaseCatalogue.diseases.first()
            .copy(name = "Previously installed built-in", description = "Retained content")
        val id = dao.insertDisease(original)
        seeder.seed()
        assertEquals(3, dao.observeDiseases().first().size)
        assertEquals(original.copy(diseaseId = id), dao.getDiseaseById(id))
        for (expected in PredefinedDiseaseCatalogue.diseases.drop(1)) {
            assertNotNull(dao.getDiseaseByCode(requireNotNull(expected.code)))
        }
    }

    @Test
    fun ordinaryCustomDiseaseIsPreserved(): Unit = runBlocking {
        val custom = PredefinedDiseaseCatalogue.diseases.first().copy(
            code = "custom_disease", name = "My disease", isPredefined = false
        )
        val id = dao.insertDisease(custom)
        seeder.seed()
        assertEquals(4, dao.observeDiseases().first().size)
        assertEquals(custom.copy(diseaseId = id), dao.getDiseaseById(id))
    }

    @Test
    fun reservedCodeConflictRollsBackEarlierInsertsAndPreservesCustomRow(): Unit = runBlocking {
        // A conflict at the LAST catalogue entry forces earlier inserts to occur
        // inside the transaction before it fails. This verifies real rollback.
        val custom = PredefinedDiseaseCatalogue.diseases.last().copy(
            name = "Legacy custom disease", isPredefined = false
        )
        val id = dao.insertDisease(custom)
        val before = dao.observeDiseases().first()
        assertFailsWith<IllegalStateException> { seeder.seed() }
        assertEquals(before, dao.observeDiseases().first())
        assertEquals(custom.copy(diseaseId = id), dao.getDiseaseById(id))
    }

    @Test
    fun ignoredInsertRollsBackEarlierInserts(): Unit = runBlocking {
        val existing = PredefinedDiseaseCatalogue.diseases.first().copy(
            diseaseId = 900L, code = "custom_existing", isPredefined = false
        )
        dao.insertDisease(existing)
        val before = dao.observeDiseases().first()
        val catalogue = PredefinedDiseaseCatalogue.diseases
        // Unique code is absent, but the explicit primary key collides. IGNORE
        // returns -1, exercising the DAO's failed-insert check and rollback.
        val conflictingBatch = listOf(
            catalogue.first(), catalogue.last().copy(diseaseId = existing.diseaseId)
        )
        assertFailsWith<IllegalStateException> {
            dao.seedPredefinedDiseases(conflictingBatch)
        }
        assertEquals(before, dao.observeDiseases().first())
    }
}
