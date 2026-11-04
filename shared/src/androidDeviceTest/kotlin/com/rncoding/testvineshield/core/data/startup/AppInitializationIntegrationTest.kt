package com.rncoding.testvineshield.core.data.startup

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.seed.DiseaseCatalogueSeeder
import com.rncoding.testvineshield.core.data.local.database.seed.PredefinedDiseaseCatalogue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AppInitializationIntegrationTest {
    private lateinit var database: VineshieldDatabase
    private lateinit var initializer: DefaultAppInitializer

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<VineshieldDatabase>(
            InstrumentationRegistry.getInstrumentation().targetContext
        ).setDriver(BundledSQLiteDriver()).build()
        initializer = DefaultAppInitializer(DiseaseCatalogueSeeder(database))
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun concurrentEntryPointsReceiveOneCompleteCatalogue(): Unit = runBlocking {
        List(10) { async { initializer.ensureInitialized() } }.awaitAll()
        val before = database.diseaseDao().observeDiseases().first()
        assertEquals(3, before.size)
        assertTrue(before.all { it.isPredefined && it.diseaseId > 0L })
        // Simulate a new process by creating a new initializer over persisted rows.
        DefaultAppInitializer(DiseaseCatalogueSeeder(database)).ensureInitialized()
        assertEquals(before, database.diseaseDao().observeDiseases().first())
    }

    @Test
    fun initializationCanRecoverAfterAConflictingLegacyRowIsRemoved(): Unit = runBlocking {
        val dao = database.diseaseDao()
        val legacy = PredefinedDiseaseCatalogue.diseases.last().copy(isPredefined = false)
        val id = dao.insertDisease(legacy)
        assertFailsWith<IllegalStateException> { initializer.ensureInitialized() }
        assertEquals(listOf(legacy.copy(diseaseId = id)), dao.observeDiseases().first())
        // Explicit test setup repair. Production startup never deletes user records.
        assertEquals(1, dao.deleteUnreferencedCustomDisease(id))
        initializer.ensureInitialized()
        assertEquals(3, dao.observeDiseases().first().size)
    }
}
