
package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Room
import androidx.room.Transactor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.entities.ActivityEntity
import com.rncoding.testvineshield.core.data.local.database.entities.BlockEntity
import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

import androidx.room.useWriterConnection

import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus


@RunWith(AndroidJUnit4::class)
class ActivityDaoTest {

    private lateinit var database: VineshieldDatabase
    private lateinit var dao: ActivityDao

    private var userId = 0L
    private var vineyardId = 0L
    private var secondVineyardId = 0L

    private val testDate = LocalDate(2026, 10, 1)

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        database = Room.inMemoryDatabaseBuilder<VineshieldDatabase>(
            context
        )
            .setDriver(BundledSQLiteDriver())
            .build()

        dao = database.activityDao()

        userId = database.userDao().insertUser(
            UserEntity(
                email = "activity-test@example.com",
                passwordHash = "test-hash",
                passwordSalt = "test-salt"
            )
        )

        vineyardId = createVineyard("First Vineyard", 0)
        secondVineyardId = createVineyard("Second Vineyard", 1)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    private suspend fun createVineyard(
        name: String,
        sortOrder: Int
    ): Long {
        return database.vineyardDao().insertVineyard(
            VineyardEntity(
                userId = userId,
                name = name,
                size = 1.0,
                country = "Bulgaria",
                city = "Plovdiv",
                latitude = 42.1,
                longitude = 24.7,
                timeZone = "Europe/Sofia",
                elevation = 200,
                sortOrder = sortOrder,
                createdAt = testDate,
                updatedAt = testDate
            )
        )
    }


    private suspend fun createBlock(
        vineyardId: Long,
        name: String
    ): Long {

        database.blockDao().upsertBlock(
            BlockEntity(
                blockId = 0L,
                vineyardId = vineyardId,
                name = name,
                area = 10,
                vineVariety = "Merlot",
                color = "Red",
                rootStock = "SO4",
                rowCount = 10,
                rowSpacing = 2,
                plantedAt = testDate,
                phenologicalStage = PhenologicalStage.DORMANCY
            )
        )

        val insertedBlock = database.blockDao()
            .getBlocksForVineyard(vineyardId)
            .single { it.name == name }

        return insertedBlock.blockId
    }


    private fun activity(
        vineyardId: Long = this.vineyardId,
        blockId: Long? = null,
        activityId: Long = 0L,
        name: String = "Pruning"
    ): ActivityEntity {
        return ActivityEntity(
            activityId = activityId,
            vineyardId = vineyardId,
            blockId = blockId,
            activityType = name,
            activityDescription = "Test activity",
            season = "2026",
            phenologicalStage = PhenologicalStage.DORMANCY,
            priority = ActivityPriority.MEDIUM,
            status = ActivityStatus.PLANNED,
            plannedAt = testDate,
            completedAt = null,
            tool = null,
            notes = null,
            createdAt = 1_000L,
            updatedAt = 1_000L,
            treatmentProduct = null,
            treatmentProductQuantity = null,
            treatmentProductUnits = null,
            waterQuantity = null,
            treatmentProductRate = null
        )
    }

    @Test
    fun createsVineyardLevelActivity(): Unit = runBlocking {
        val id = dao.insertActivityForVineyard(
            activity(blockId = null)
        )

        assertNotNull(id)
        assertTrue(id > 0L)

        val stored = dao.getActivityById(vineyardId, id)

        assertNotNull(stored)
        assertEquals(vineyardId, stored.vineyardId)
        assertNull(stored.blockId)
        assertEquals("Pruning", stored.activityType)
    }

    @Test
    fun createsActivityWithBlockFromSameVineyard(): Unit =
        runBlocking {
            val blockId = createBlock(
                vineyardId = vineyardId,
                name = "Block A"
            )

            val id = dao.insertActivityForVineyard(
                activity(blockId = blockId)
            )

            assertNotNull(id)

            val stored = dao.getActivityById(vineyardId, id)

            assertNotNull(stored)
            assertEquals(blockId, stored.blockId)
        }

    @Test
    fun rejectsBlockFromAnotherVineyard(): Unit = runBlocking {
        val otherBlockId = createBlock(
            vineyardId = secondVineyardId,
            name = "Other Vineyard Block"
        )

        val result = dao.insertActivityForVineyard(
            activity(
                vineyardId = vineyardId,
                blockId = otherBlockId
            )
        )

        assertNull(result)
        assertEquals(
            0,
            dao.countAllActivitiesForVineyard(vineyardId)
        )
    }

    @Test
    fun updateCannotMoveActivityToAnotherVineyard(): Unit =
        runBlocking {
            val id = dao.insertActivityForVineyard(
                activity()
            )

            assertNotNull(id)

            val changed = dao.updateActivityForVineyard(
                vineyardId = vineyardId,
                activity = activity(
                    vineyardId = secondVineyardId,
                    activityId = id,
                    name = "Unauthorized change"
                )
            )

            assertFalse(changed)

            val original = dao.getActivityById(
                vineyardId = vineyardId,
                activityId = id
            )

            assertNotNull(original)
            assertEquals("Pruning", original.activityType)
            assertEquals(vineyardId, original.vineyardId)
        }

    @Test
    fun updateCannotInsertMissingActivity(): Unit = runBlocking {
        val updated = dao.updateActivityForVineyard(
            vineyardId = vineyardId,
            activity = activity(
                activityId = 999_999L
            )
        )

        assertFalse(updated)
        assertNull(
            dao.getActivityById(
                vineyardId = vineyardId,
                activityId = 999_999L
            )
        )
    }

    @Test
    fun deleteIsRestrictedToRequestedVineyard(): Unit =
        runBlocking {
            val id = dao.insertActivityForVineyard(
                activity(vineyardId = vineyardId)
            )

            assertNotNull(id)

            val wrongVineyardResult = dao.deleteActivitySafely(
                vineyardId = secondVineyardId,
                activityId = id
            )

            assertEquals(0, wrongVineyardResult)
            assertNotNull(dao.getActivityById(vineyardId, id))

            val correctVineyardResult = dao.deleteActivitySafely(
                vineyardId = vineyardId,
                activityId = id
            )

            assertEquals(1, correctVineyardResult)
            assertNull(dao.getActivityById(vineyardId, id))
        }

    @Test
    fun deleteMissingActivityReturnsNotFound(): Unit =
        runBlocking {
            val result = dao.deleteActivitySafely(
                vineyardId = vineyardId,
                activityId = 999_999L
            )

            assertEquals(0, result)
        }

    @Test
    fun updateRejectsBlockFromAnotherVineyard(): Unit =
        runBlocking {
            val id = dao.insertActivityForVineyard(
                activity()
            )
            assertNotNull(id)

            val foreignBlockId = createBlock(
                vineyardId = secondVineyardId,
                name = "Foreign Block"
            )

            val updated = dao.updateActivityForVineyard(
                vineyardId = vineyardId,
                activity = activity(
                    activityId = id,
                    blockId = foreignBlockId
                )
            )

            assertFalse(updated)

            val stored = dao.getActivityById(vineyardId, id)
            assertNotNull(stored)
            assertNull(stored.blockId)
        }



    private suspend fun createDiseaseLink(
        activityId: Long,
        vineyardId: Long = this.vineyardId
    ): Long {
        val diseaseId = 9001L
        val occurrenceId = 9002L

        database.diseaseDao().upsertDisease(
            DiseaseEntity(
                diseaseId = diseaseId,
                code = "test-disease",
                name = "Test Disease",
                description = "Test description",
                vectors = "",
                factors = "",
                treatmentSuggestion = ""
            )
        )

        database.diseaseOccurrenceDao().upsertOccurrence(
            DiseaseOccurrenceEntity(
                occurrenceId = occurrenceId,
                diseaseId = diseaseId,
                vineyardId = vineyardId,
                blockId = null,
                observedAt = testDate,
                severity = 10,
                status = DiseaseOccurrenceStatus.ACTIVE,
                curedAt = null,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                notes = "Test occurrence"
            )
        )

        // The association must not exist before insertion.
        assertFalse(
            dao.hasDiseaseOccurrenceLinks(activityId),
            "Unexpected disease association before test fixture insertion."
        )

        // Insert the association exactly once.
        database.useWriterConnection { transactor ->
            transactor.withTransaction(
                type = Transactor.SQLiteTransactionType.IMMEDIATE
            ) {
                usePrepared(
                    """
                INSERT INTO OccurrenceActivityCrossRef
                    (occurrence_id, activity_id, applied_at, created_at, notes)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
                ) { statement ->
                    statement.bindLong(1, occurrenceId)
                    statement.bindLong(2, activityId)
                    statement.bindText(3, "2026-10-01")
                    statement.bindText(4, "2026-10-01")
                    statement.bindText(5, "Test treatment")
                    statement.step()
                }
            }
        }

        // Verify that the association was created.
        assertTrue(
            dao.hasDiseaseOccurrenceLinks(activityId),
            "Disease association was not created."
        )

        return occurrenceId
    }


    @Test
    fun linkedActivityCannotBeDeleted(): Unit = runBlocking {
        val activityId = dao.insertActivityForVineyard(
            activity()
        )
        assertNotNull(activityId)

        val occurrenceId = createDiseaseLink(
            activityId = activityId
        )

        assertTrue(
            dao.hasDiseaseOccurrenceLinks(activityId)
        )

        val deletionResult = dao.deleteActivitySafely(
            vineyardId = vineyardId,
            activityId = activityId
        )

        assertEquals(2, deletionResult)

        assertNotNull(
            dao.getActivityById(vineyardId, activityId)
        )

        val occurrence = database.diseaseOccurrenceDao()
            .getDiseaseOccurrenceById(occurrenceId)

        assertNotNull(occurrence)

        assertTrue(
            dao.hasDiseaseOccurrenceLinks(activityId)
        )
    }


    @Test
    fun activityCanBeDeletedAfterDiseaseLinkIsRemoved(): Unit =
        runBlocking {
            val activityId = dao.insertActivityForVineyard(
                activity()
            )
            assertNotNull(activityId)

            val occurrenceId = createDiseaseLink(activityId)

            database.diseaseOccurrenceDao()
                .deleteDiseaseOccurrenceById(occurrenceId)

            assertFalse(
                dao.hasDiseaseOccurrenceLinks(activityId)
            )

            val result = dao.deleteActivitySafely(
                vineyardId = vineyardId,
                activityId = activityId
            )

            assertEquals(1, result)

            assertNull(
                dao.getActivityById(vineyardId, activityId)
            )
        }

}
