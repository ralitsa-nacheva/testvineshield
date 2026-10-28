
package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Room
import androidx.room.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import junit.framework.TestCase.assertEquals

import com.rncoding.testvineshield.core.data.local.database.entities.BlockEntity
import com.rncoding.testvineshield.core.data.local.database.entities.ActivityEntity
import com.rncoding.testvineshield.core.data.local.database.entities.HarvestEntity
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage

import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseAlertEntity

import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertSeverity

import kotlinx.coroutines.flow.first
import kotlin.test.assertNull
import kotlin.test.assertTrue

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class BlockCascadeDeletionTest {

    private lateinit var database: VineshieldDatabase

    private var userId = 0L
    private var vineyardId = 0L

    private val testDate = LocalDate(2026, 10, 1)

    @Before
    fun setUp(): Unit = runBlocking {

        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        database = Room.inMemoryDatabaseBuilder<VineshieldDatabase>(
            context
        )
            .setDriver(BundledSQLiteDriver())
            .build()

        userId = database.userDao().insertUser(
            UserEntity(
                email = "block-cascade-test@example.com",
                passwordHash = "test-hash",
                passwordSalt = "test-salt"
            )
        )

        vineyardId = database.vineyardDao().insertVineyard(
            VineyardEntity(
                userId = userId,
                name = "Cascade Test Vineyard",
                size = 1.0,
                country = "Bulgaria",
                city = "Plovdiv",
                latitude = 42.1,
                longitude = 24.7,
                timeZone = "Europe/Sofia",
                elevation = 200,
                sortOrder = 0,
                createdAt = testDate,
                updatedAt = testDate
            )
        )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun databaseFixtureIsInitialized(): Unit = runBlocking {
        val vineyard = database.vineyardDao().getVineyard(
            userId = userId,
            vineyardId = vineyardId
        )

        assertNotNull(vineyard)
        assertEquals(vineyardId, vineyard.vineyardId)
        assertEquals(userId, vineyard.userId)

    }


    @Test
    fun deletingBlockCascadesActivitiesAndHarvestsButPreservesOtherRecords(): Unit =
        runBlocking {

            val blockDao = database.blockDao()
            val activityDao = database.activityDao()
            val harvestDao = database.harvestDao()

            // 1. Create two blocks in the same vineyard.
            fun newBlock(name: String) = BlockEntity(
                blockId = 0L,
                vineyardId = vineyardId,
                name = name,
                area = 1000,
                vineVariety = "Merlot",
                color = "Red",
                rootStock = "SO4",
                rowCount = 20,
                rowSpacing = 250,
                plantedAt = testDate,
                phenologicalStage = null
            )

            val blockAId = blockDao.insertBlock(
                newBlock("Block A")
            )

            val blockBId = blockDao.insertBlock(
                newBlock("Block B")
            )

            // 2. Create an activity for each block,
            // plus one vineyard-level activity.
            fun newActivity(
                blockId: Long?,
                description: String
            ) = ActivityEntity(
                activityId = 0L,
                vineyardId = vineyardId,
                blockId = blockId,
                activityType = "inspection",
                activityDescription = description,
                season = "2026",
                phenologicalStage = PhenologicalStage.HARVEST,
                priority = ActivityPriority.MEDIUM,
                status = ActivityStatus.PLANNED,
                plannedAt = testDate,
                completedAt = null,
                tool = null,
                notes = null,
                createdAt = 1L,
                updatedAt = 1L,
                treatmentProduct = null,
                treatmentProductQuantity = null,
                treatmentProductUnits = null,
                waterQuantity = null,
                treatmentProductRate = null
            )

            val activityAId = activityDao.insertActivity(
                newActivity(blockAId, "Inspect Block A")
            )

            val activityBId = activityDao.insertActivity(
                newActivity(blockBId, "Inspect Block B")
            )

            val vineyardActivityId = activityDao.insertActivity(
                newActivity(null, "Inspect vineyard")
            )

            // 3. Create a harvest for each block.
            fun newHarvest(blockId: Long) = HarvestEntity(
                harvestId = 0L,
                blockId = blockId,
                season = "2026",
                harvestDate = testDate,
                units = "kg",
                quantity = 500.00,
                brixSugarContent = 22.00
            )

            harvestDao.upsertHarvest(newHarvest(blockAId))
            harvestDao.upsertHarvest(newHarvest(blockBId))

            val countHarvestsForBlock =
                "SELECT COUNT(*) FROM harvest WHERE block_id = ?"

            assertEquals(
                1L,
                countRows(countHarvestsForBlock, blockAId)
            )

            assertEquals(
                1L,
                countRows(countHarvestsForBlock, blockBId)
            )

            // 4. Verify the records exist before deletion.
            assertEquals(
                1,
                blockDao.getBlockWithHarvest(blockAId)
                    .single()
                    .harvest.size
            )

            assertEquals(
                1,
                blockDao.getBlockWithHarvest(blockBId)
                    .single()
                    .harvest.size
            )

            // 5. Delete Block A.
            val deletedRows = blockDao.deleteBlockById(
                vineyardId = vineyardId,
                blockId = blockAId
            )

            assertEquals(1, deletedRows)

            // Block A's harvest row must be physically deleted.
            assertEquals(
                0L,
                countRows(countHarvestsForBlock, blockAId)
            )

// Block B's harvest must remain.
            assertEquals(
                1L,
                countRows(countHarvestsForBlock, blockBId)
            )

// 6. Block A is gone; Block B survives.
            assertNull(blockDao.getBlockById(vineyardId, blockAId))
            assertNotNull(blockDao.getBlockById(vineyardId, blockBId))

// 7. Block A's activity was deleted.
            assertNull(
                activityDao.getActivityById(vineyardId, activityAId)
            )

// Block B's activity survives.
            assertNotNull(
                activityDao.getActivityById(vineyardId, activityBId)
            )

// Vineyard-level activity survives.
            assertNotNull(
                activityDao.getActivityById(vineyardId, vineyardActivityId)
            )

// 8. Only two activities remain.
            assertEquals(
                2,
                activityDao.countAllActivitiesForVineyard(vineyardId)
            )

// 9. Block A's harvest relation is gone.
            assertTrue(
                blockDao.getBlockWithHarvest(blockAId).isEmpty()
            )

// Block B and its harvest survive.
            assertEquals(
                1,
                blockDao.getBlockWithHarvest(blockBId)
                    .single()
                    .harvest.size
            )
        }


    @Test
    fun deletingBlockCascadesDiseaseOccurrencesAndAlerts(): Unit =
        runBlocking {

            val blockDao = database.blockDao()
            val diseaseDao = database.diseaseDao()
            val occurrenceDao = database.diseaseOccurrenceDao()
            val alertDao = database.diseaseAlertDao()

            // 1. Create two blocks.
            fun newBlock(name: String) = BlockEntity(
                blockId = 0L,
                vineyardId = vineyardId,
                name = name,
                area = 1000,
                vineVariety = "Merlot",
                color = "Red",
                rootStock = "SO4",
                rowCount = 20,
                rowSpacing = 250,
                plantedAt = testDate,
                phenologicalStage = null
            )

            val blockAId = blockDao.insertBlock(newBlock("Block A"))
            val blockBId = blockDao.insertBlock(newBlock("Block B"))

            // 2. Create a disease knowledge-base record.
            // Explicit ID lets us reference it in the other entities.
            val diseaseId = 100L

            diseaseDao.insertDisease(
                DiseaseEntity(
                    diseaseId = diseaseId,
                    code = "DM",
                    name = "Downy mildew",
                    description = "Test disease",
                    vectors = "",
                    factors = "",
                    treatmentSuggestion = ""
                )
            )

            // 3. Create occurrences for Block A, Block B,
            // and the vineyard itself.
            fun newOccurrence(
                id: Long,
                blockId: Long?
            ) = DiseaseOccurrenceEntity(
                occurrenceId = id,
                diseaseId = diseaseId,
                vineyardId = vineyardId,
                blockId = blockId,
                observedAt = testDate,
                severity = 20,
                status = DiseaseOccurrenceStatus.ACTIVE,
                curedAt = null,
                createdAt = 1L,
                updatedAt = 1L,
                notes = "Cascade test"
            )

            val occurrenceAId = 101L
            val occurrenceBId = 102L
            val vineyardOccurrenceId = 103L

            occurrenceDao.upsertOccurrence(
                newOccurrence(occurrenceAId, blockAId)
            )
            occurrenceDao.upsertOccurrence(
                newOccurrence(occurrenceBId, blockBId)
            )
            occurrenceDao.upsertOccurrence(
                newOccurrence(vineyardOccurrenceId, null)
            )

            // 4. Create alerts for Block A, Block B,
            // and the vineyard itself.
            fun newAlert(
                id: Long,
                blockId: Long?
            ) = DiseaseAlertEntity(
                alertId = id,
                diseaseId = diseaseId,
                vineyardId = vineyardId,
                blockId = blockId,
                createdAt = testDate,
                alert = "Downy mildew risk",
                alertStatus = AlertStatus.ACTIVE,
                alertSeverity = AlertSeverity.MEDIUM,
                phenologicalStage = PhenologicalStage.HARVEST
            )

            val alertAId = 201L
            val alertBId = 202L
            val vineyardAlertId = 203L

            alertDao.upsertDiseaseAlert(
                newAlert(alertAId, blockAId)
            )
            alertDao.upsertDiseaseAlert(
                newAlert(alertBId, blockBId)
            )
            alertDao.upsertDiseaseAlert(
                newAlert(vineyardAlertId, null)
            )

            // 5. Verify all records exist before deletion.
            assertEquals(
                3,
                occurrenceDao.countDiseaseOccurrencesForVineyard(vineyardId)
            )

            assertEquals(
                3,
                alertDao.getAllAlertsForVineyard(vineyardId).size
            )

            // 6. Delete Block A.
            val deletedRows = blockDao.deleteBlockById(
                vineyardId = vineyardId,
                blockId = blockAId
            )

            assertEquals(1, deletedRows)

            // 7. Verify block deletion and preservation.
            assertNull(
                blockDao.getBlockById(vineyardId, blockAId)
            )
            assertNotNull(
                blockDao.getBlockById(vineyardId, blockBId)
            )

            // 8. Verify Block A's occurrence was deleted.
            assertNull(
                occurrenceDao.getDiseaseOccurrenceById(occurrenceAId)
            )

            // Block B's occurrence survives.
            assertNotNull(
                occurrenceDao.getDiseaseOccurrenceById(occurrenceBId)
            )

            // Vineyard-level occurrence survives.
            assertNotNull(
                occurrenceDao.getDiseaseOccurrenceById(vineyardOccurrenceId)
            )

            assertEquals(
                2,
                occurrenceDao.countDiseaseOccurrencesForVineyard(vineyardId)
            )

            // 9. Verify disease alerts.
            val remainingAlerts =
                alertDao.getAllAlertsForVineyard(vineyardId)

            assertEquals(2, remainingAlerts.size)

            assertTrue(remainingAlerts.none { it.alertId == alertAId })
            assertTrue(remainingAlerts.any { it.alertId == alertBId })
            assertTrue(remainingAlerts.any { it.alertId == vineyardAlertId })

            // 10. The shared disease definition remains intact.
            val remainingDiseases =
                diseaseDao.observeDiseases().first()

            assertTrue(
                remainingDiseases.any { it.diseaseId == diseaseId }
            )
        }



    private suspend fun countRows(
        sql: String,
        id: Long
    ): Long = database.useReaderConnection { connection ->
        connection.usePrepared(sql) { statement ->
            statement.bindLong(1, id)

            if (statement.step()) {
                statement.getLong(0)
            } else {
                0L
            }
        }
    }


    @Test
    fun harvestStoresDecimalQuantityAndBrix(): Unit = runBlocking {
        val blockId = database.blockDao().insertBlock(
            BlockEntity(
                blockId = 0L,
                vineyardId = vineyardId,
                name = "Decimal Harvest Test",
                area = 1000,
                vineVariety = "Merlot",
                color = "Red",
                rootStock = "SO4",
                rowCount = 20,
                rowSpacing = 250,
                plantedAt = testDate,
                phenologicalStage = null
            )
        )

        database.harvestDao().upsertHarvest(
            HarvestEntity(
                harvestId = 0L,
                blockId = blockId,
                season = "2026",
                harvestDate = testDate,
                units = "kg",
                quantity = 1250.75,
                brixSugarContent = 22.5
            )
        )

        val savedHarvest = database.harvestDao()
            .observeHarvestsForBlock(blockId)
            .first()
            .single()

        assertEquals(1250.75, savedHarvest.quantity, 0.000001)
        assertEquals(22.5, savedHarvest.brixSugarContent, 0.000001)
    }


}
