
package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase

import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity

import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseAlertEntity
import com.rncoding.testvineshield.core.data.local.mappers.DiseaseAlertMapper
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertSeverity
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import kotlinx.datetime.LocalDate

import com.rncoding.testvineshield.core.data.local.mappers.DiseaseMapper
import com.rncoding.testvineshield.core.data.local.mappers.DiseaseOccurrenceMapper
import com.rncoding.testvineshield.core.data.local.mappers.SymptomMapper
import com.rncoding.testvineshield.core.data.repository.DiseaseRepositoryImpl
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DiseaseDaoIntegrationTest {

    private lateinit var database: VineshieldDatabase
    private lateinit var diseaseDao: DiseaseDao

    private lateinit var diseaseOccurrenceDao: DiseaseOccurrenceDao
    private lateinit var symptomDao: SymptomDao
    private lateinit var diseaseAlertDao: DiseaseAlertDao

    private lateinit var diseaseOccurrenceMapper: DiseaseOccurrenceMapper

    private lateinit var symptomMapper: SymptomMapper
    private lateinit var diseaseAlertMapper: DiseaseAlertMapper

    @Before
    fun setUp() {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        database = Room.inMemoryDatabaseBuilder<VineshieldDatabase>(
            context
        )
            .setDriver(BundledSQLiteDriver())
            .build()

        diseaseDao = database.diseaseDao()
        diseaseOccurrenceDao = database.diseaseOccurrenceDao()
        symptomDao = database.symptomDao()
        diseaseAlertDao = database.diseaseAlertDao()

    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    private fun createDiseaseRepository() = DiseaseRepositoryImpl(
        diseaseDao = diseaseDao,
        diseaseOccurrenceDao = diseaseOccurrenceDao,
        symptomDao = symptomDao,
        diseaseAlertDao = diseaseAlertDao,
        diseaseMapper = DiseaseMapper(),
        diseaseOccurrenceMapper = DiseaseOccurrenceMapper(),
        symptomMapper = SymptomMapper(),
        diseaseAlertMapper = DiseaseAlertMapper()

    )

    private fun newDisease(
        name: String = "Custom disease",
        isPredefined: Boolean = false
    ) = DiseaseEntity(
        diseaseId = 0L,
        code = null,
        name = name,
        description = "Original description",
        vectors = "Original vectors",
        factors = "Original factors",
        treatmentSuggestion = "Original treatment",
        isPredefined = isPredefined
    )

    @Test
    fun insertCustomDiseaseAndRetrieveById(): Unit = runBlocking {
        val diseaseId = diseaseDao.insertDisease(
            newDisease()
        )

        assertTrue(diseaseId > 0L)

        val stored = diseaseDao.getDiseaseById(diseaseId)

        assertNotNull(stored)
        assertEquals(diseaseId, stored.diseaseId)
        assertEquals("Custom disease", stored.name)
        assertFalse(stored.isPredefined)
    }

    @Test
    fun observeDiseasesIncludesPredefinedAndCustom(): Unit = runBlocking {
        diseaseDao.insertDisease(
            newDisease(
                name = "Powdery mildew",
                isPredefined = true
            )
        )

        diseaseDao.insertDisease(
            newDisease(
                name = "Custom fungal disease"
            )
        )

        val diseases = diseaseDao.observeDiseases().first()

        assertEquals(2, diseases.size)
        assertEquals(
            listOf("Custom fungal disease", "Powdery mildew"),
            diseases.map { it.name }
        )
        assertEquals(1, diseases.count { it.isPredefined })
        assertEquals(1, diseases.count { !it.isPredefined })
    }

    @Test
    fun updateCustomDiseaseChangesEditableFields(): Unit = runBlocking {
        val diseaseId = diseaseDao.insertDisease(
            newDisease()
        )

        val updatedRows = diseaseDao.updateCustomDisease(
            diseaseId = diseaseId,
            code = "CUSTOM-01",
            name = "Updated disease",
            description = "Updated description",
            vectors = "Updated vectors",
            factors = "Updated factors",
            treatmentSuggestion = "Updated treatment"
        )

        assertEquals(1, updatedRows)

        val stored = diseaseDao.getDiseaseById(diseaseId)

        assertNotNull(stored)
        assertEquals("CUSTOM-01", stored.code)
        assertEquals("Updated disease", stored.name)
        assertEquals("Updated description", stored.description)
        assertEquals("Updated vectors", stored.vectors)
        assertEquals("Updated factors", stored.factors)
        assertEquals("Updated treatment", stored.treatmentSuggestion)
        assertFalse(stored.isPredefined)
    }

    @Test
    fun updatePredefinedDiseaseIsRejected(): Unit = runBlocking {
        val diseaseId = diseaseDao.insertDisease(
            newDisease(
                name = "Protected disease",
                isPredefined = true
            )
        )

        val updatedRows = diseaseDao.updateCustomDisease(
            diseaseId = diseaseId,
            code = "CHANGED",
            name = "Modified disease",
            description = "Modified description",
            vectors = "Modified vectors",
            factors = "Modified factors",
            treatmentSuggestion = "Modified treatment"
        )

        assertEquals(0, updatedRows)

        val stored = diseaseDao.getDiseaseById(diseaseId)

        assertNotNull(stored)
        assertEquals("Protected disease", stored.name)
        assertEquals("Original description", stored.description)
        assertTrue(stored.isPredefined)
    }

    @Test
    fun deleteUnreferencedCustomDiseaseSucceeds(): Unit = runBlocking {
        val diseaseId = diseaseDao.insertDisease(
            newDisease()
        )

        val deletedRows =
            diseaseDao.deleteUnreferencedCustomDisease(diseaseId)

        assertEquals(1, deletedRows)
        assertNull(diseaseDao.getDiseaseById(diseaseId))
    }

    @Test
    fun deletePredefinedDiseaseIsRejected(): Unit = runBlocking {
        val diseaseId = diseaseDao.insertDisease(
            newDisease(
                name = "Protected disease",
                isPredefined = true
            )
        )

        val deletedRows =
            diseaseDao.deleteUnreferencedCustomDisease(diseaseId)

        assertEquals(0, deletedRows)

        val stored = diseaseDao.getDiseaseById(diseaseId)

        assertNotNull(stored)
        assertTrue(stored.isPredefined)
    }

    @Test
    fun updateNonexistentDiseaseReturnsZero(): Unit = runBlocking {
        val updatedRows = diseaseDao.updateCustomDisease(
            diseaseId = 99999L,
            code = null,
            name = "Missing",
            description = "",
            vectors = "",
            factors = "",
            treatmentSuggestion = ""
        )

        assertEquals(0, updatedRows)
    }

    @Test
    fun deleteNonexistentDiseaseReturnsZero(): Unit = runBlocking {
        val deletedRows =
            diseaseDao.deleteUnreferencedCustomDisease(99999L)

        assertEquals(0, deletedRows)
    }


    @Test
    fun deleteCustomDiseaseReferencedByOccurrenceIsRejected(): Unit = runBlocking {
        val vineyardId = createTestVineyard()

        val diseaseId = diseaseDao.insertDisease(
            newDisease(name = "Disease with occurrence")
        )

        database.diseaseOccurrenceDao().upsertOccurrence(
            DiseaseOccurrenceEntity(
                occurrenceId = 0L,
                diseaseId = diseaseId,
                vineyardId = vineyardId,
                blockId = null,
                observedAt = LocalDate(2026, 10, 1),
                severity = 30,
                status = DiseaseOccurrenceStatus.ACTIVE,
                curedAt = null,
                createdAt = 1L,
                updatedAt = 1L,
                notes = "Observed symptoms"
            )
        )

        val deletedRows =
            diseaseDao.deleteUnreferencedCustomDisease(diseaseId)

        assertEquals(0, deletedRows)

        val storedDisease = diseaseDao.getDiseaseById(diseaseId)
        assertNotNull(storedDisease)

        val occurrences = database
            .diseaseOccurrenceDao()
            .observeOccurrences()
            .first()

        assertEquals(1, occurrences.size)
        assertEquals(diseaseId, occurrences.single().diseaseId)
    }


    @Test
    fun deleteCustomDiseaseReferencedByAlertIsRejected(): Unit = runBlocking {
        val vineyardId = createTestVineyard()

        val diseaseId = diseaseDao.insertDisease(
            newDisease(name = "Disease with alert")
        )

        database.diseaseAlertDao().upsertDiseaseAlert(
            DiseaseAlertEntity(
                alertId = 0L,
                diseaseId = diseaseId,
                vineyardId = vineyardId,
                blockId = null,
                createdAt = LocalDate(2026, 10, 1),
                alert = "High disease risk",
                alertStatus = AlertStatus.ACTIVE,
                alertSeverity = AlertSeverity.HIGH,
                phenologicalStage = PhenologicalStage.FLOWERING
            )
        )

        val deletedRows =
            diseaseDao.deleteUnreferencedCustomDisease(diseaseId)

        assertEquals(0, deletedRows)

        val storedDisease = diseaseDao.getDiseaseById(diseaseId)
        assertNotNull(storedDisease)

        val alerts = database
            .diseaseAlertDao()
            .getAllAlertsForVineyard(vineyardId)

        assertEquals(1, alerts.size)
        assertEquals(diseaseId, alerts.single().diseaseId)
    }


    @Test
    fun repositoryCreatesCustomDisease(): Unit = runBlocking {
        val repository = createDiseaseRepository()

        val disease = DiseaseDomainModel(
            diseaseId = 0L,
            code = "CUSTOM-01",
            name = "Custom grapevine disease",
            description = "Test description",
            vectors = "Test vectors",
            factors = "Test factors",
            treatmentSuggestion = "Test treatment",
            isPredefined = false
        )

        val diseaseId = repository.createDisease(disease)

        assertTrue(diseaseId > 0L)

        val stored = repository.getDisease(diseaseId)

        assertNotNull(stored)
        assertEquals(diseaseId, stored.diseaseId)
        assertEquals(disease.code, stored.code)
        assertEquals(disease.name, stored.name)
        assertEquals(disease.description, stored.description)
        assertEquals(disease.vectors, stored.vectors)
        assertEquals(disease.factors, stored.factors)
        assertEquals(
            disease.treatmentSuggestion,
            stored.treatmentSuggestion
        )
        assertFalse(stored.isPredefined)
    }

    @Test
    fun repositoryCannotCreatePredefinedDisease(): Unit = runBlocking {
        val repository = createDiseaseRepository()

        val diseaseId = repository.createDisease(
            DiseaseDomainModel(
                diseaseId = 0L,
                code = "BUILTIN-TEST",
                name = "Attempted predefined disease",
                description = "Test description",
                vectors = "Test vectors",
                factors = "Test factors",
                treatmentSuggestion = "Test treatment",
                isPredefined = true
            )
        )

        val stored = repository.getDisease(diseaseId)

        assertNotNull(stored)
        assertFalse(stored.isPredefined)

        val storedEntity = diseaseDao.getDiseaseById(diseaseId)

        assertNotNull(storedEntity)
        assertFalse(storedEntity.isPredefined)
    }

    @Test
    fun repositoryObservesPredefinedAndCustomDiseases(): Unit = runBlocking {
        val repository = createDiseaseRepository()

        diseaseDao.insertDisease(
            newDisease(
                name = "Predefined mildew",
                isPredefined = true
            )
        )

        diseaseDao.insertDisease(
            newDisease(
                name = "Custom mildew",
                isPredefined = false
            )
        )

        val diseases = repository.observeDiseaseCatalog().first()

        assertEquals(2, diseases.size)

        assertTrue(
            diseases.any {
                it.name == "Predefined mildew" && it.isPredefined
            }
        )

        assertTrue(
            diseases.any {
                it.name == "Custom mildew" && !it.isPredefined
            }
        )
    }

    @Test
    fun repositoryUpdatesCustomDisease(): Unit = runBlocking {
        val repository = createDiseaseRepository()

        val diseaseId = repository.createDisease(
            DiseaseDomainModel(
                diseaseId = 0L,
                code = null,
                name = "Original disease",
                description = "Original description",
                vectors = "Original vectors",
                factors = "Original factors",
                treatmentSuggestion = "Original treatment",
                isPredefined = false
            )
        )

        val original = repository.getDisease(diseaseId)
        assertNotNull(original)

        val updated = repository.updateDisease(
            original.copy(
                code = "UPDATED-01",
                name = "Updated disease",
                description = "Updated description",
                treatmentSuggestion = "Updated treatment"
            )
        )

        assertTrue(updated)

        val stored = repository.getDisease(diseaseId)

        assertNotNull(stored)
        assertEquals("UPDATED-01", stored.code)
        assertEquals("Updated disease", stored.name)
        assertEquals("Updated description", stored.description)
        assertEquals("Updated treatment", stored.treatmentSuggestion)
        assertEquals("Original vectors", stored.vectors)
        assertEquals("Original factors", stored.factors)
        assertFalse(stored.isPredefined)
    }

    @Test
    fun repositoryDeletesUnreferencedCustomDisease(): Unit = runBlocking {
        val repository = createDiseaseRepository()

        val diseaseId = repository.createDisease(
            DiseaseDomainModel(
                diseaseId = 0L,
                code = null,
                name = "Disease to delete",
                description = "Test description",
                vectors = "Test vectors",
                factors = "Test factors",
                treatmentSuggestion = "Test treatment",
                isPredefined = false
            )
        )

        assertNotNull(repository.getDisease(diseaseId))

        val deleted = repository.deleteDisease(diseaseId)

        assertTrue(deleted)
        assertNull(repository.getDisease(diseaseId))
    }


    @Test
    fun repositoryCannotUpdatePredefinedDisease(): Unit = runBlocking {
        val repository = createDiseaseRepository()

        val diseaseId = diseaseDao.insertDisease(
            newDisease(
                name = "Protected disease",
                isPredefined = true
            )
        )

        val original = repository.getDisease(diseaseId)
        assertNotNull(original)

        val updated = repository.updateDisease(
            original.copy(
                name = "Unauthorized change",
                description = "Changed description"
            )
        )

        assertFalse(updated)

        val stored = repository.getDisease(diseaseId)
        assertNotNull(stored)

        assertEquals("Protected disease", stored.name)
        assertEquals("Original description", stored.description)
        assertTrue(stored.isPredefined)
    }

    @Test
    fun repositoryCannotDeletePredefinedDisease(): Unit = runBlocking {
        val repository = createDiseaseRepository()

        val diseaseId = diseaseDao.insertDisease(
            newDisease(
                name = "Protected disease",
                isPredefined = true
            )
        )

        val deleted = repository.deleteDisease(diseaseId)

        assertFalse(deleted)

        val stored = repository.getDisease(diseaseId)
        assertNotNull(stored)
        assertTrue(stored.isPredefined)
    }

    @Test
    fun repositoryCannotDeleteDiseaseReferencedByOccurrence(): Unit = runBlocking {
        val repository = createDiseaseRepository()
        val vineyardId = createTestVineyard()

        val diseaseId = diseaseDao.insertDisease(
            newDisease(name = "Disease with occurrence")
        )

        diseaseOccurrenceDao.upsertOccurrence(
            DiseaseOccurrenceEntity(
                occurrenceId = 0L,
                diseaseId = diseaseId,
                vineyardId = vineyardId,
                blockId = null,
                observedAt = LocalDate(2026, 10, 1),
                severity = 30,
                status = DiseaseOccurrenceStatus.ACTIVE,
                curedAt = null,
                createdAt = 1L,
                updatedAt = 1L,
                notes = "Test occurrence"
            )
        )

        val deleted = repository.deleteDisease(diseaseId)

        assertTrue(
            diseaseDao.isDiseaseReferenced(diseaseId)
        )

        assertFalse(deleted)
        assertNotNull(repository.getDisease(diseaseId))

        val occurrences = diseaseOccurrenceDao
            .observeOccurrences()
            .first()

        assertEquals(1, occurrences.size)
        assertEquals(diseaseId, occurrences.single().diseaseId)
    }

    @Test
    fun repositoryCannotDeleteDiseaseReferencedByAlert(): Unit = runBlocking {
        val repository = createDiseaseRepository()
        val vineyardId = createTestVineyard()

        val diseaseId = diseaseDao.insertDisease(
            newDisease(name = "Disease with alert")
        )

        diseaseAlertDao.upsertDiseaseAlert(
            DiseaseAlertEntity(
                alertId = 0L,
                diseaseId = diseaseId,
                vineyardId = vineyardId,
                blockId = null,
                createdAt = LocalDate(2026, 10, 1),
                alert = "Disease risk",
                alertStatus = AlertStatus.ACTIVE,
                alertSeverity = AlertSeverity.HIGH,
                phenologicalStage = PhenologicalStage.FLOWERING
            )
        )

        val deleted = repository.deleteDisease(diseaseId)

        assertTrue(
            diseaseDao.isDiseaseReferenced(diseaseId)
        )

        assertFalse(deleted)
        assertNotNull(repository.getDisease(diseaseId))

        val alerts = diseaseAlertDao
            .getAllAlertsForVineyard(vineyardId)

        assertEquals(1, alerts.size)
        assertEquals(diseaseId, alerts.single().diseaseId)
    }



    @Test
    fun unreferencedDiseaseReturnsFalse() = runBlocking {
        val diseaseId = diseaseDao.insertDisease(
            newDisease()
        )

        assertFalse(
            diseaseDao.isDiseaseReferenced(diseaseId)
        )
    }

    @Test
    fun missingDiseaseReturnsFalseForReferenceCheck() = runBlocking {
        assertFalse(
            diseaseDao.isDiseaseReferenced(Long.MAX_VALUE)
        )
    }




    private suspend fun createTestVineyard(): Long {
        val userId = database.userDao().insertUser(
            UserEntity(
                email = "disease-dao-test@example.com",
                passwordHash = "test-hash",
                passwordSalt = "test-salt"
            )
        )

        return database.vineyardDao().insertVineyard(
            VineyardEntity(
                userId = userId,
                name = "Disease Test Vineyard",
                size = 1.0,
                country = "Bulgaria",
                city = "Plovdiv",
                latitude = 42.1,
                longitude = 24.7,
                timeZone = "Europe/Sofia",
                elevation = 200,
                sortOrder = 0,
                createdAt = LocalDate(2026, 10, 1),
                updatedAt = LocalDate(2026, 10, 1)
            )
        )
    }

}
