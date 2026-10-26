
package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Room
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
                quantity = 500,
                brixSugarContent = 22
            )

            harvestDao.upsertHarvest(newHarvest(blockAId))
            harvestDao.upsertHarvest(newHarvest(blockBId))

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

}
