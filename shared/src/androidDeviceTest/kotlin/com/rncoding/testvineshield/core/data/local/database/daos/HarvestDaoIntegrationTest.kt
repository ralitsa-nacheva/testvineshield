
package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.entities.BlockEntity
import com.rncoding.testvineshield.core.data.local.database.entities.HarvestEntity
import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.data.local.mappers.HarvestMapper
import com.rncoding.testvineshield.core.data.repository.HarvestRepositoryImpl
import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.datetime.LocalDate

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class HarvestDaoIntegrationTest {

    private lateinit var database: VineshieldDatabase

    private var vineyardId = 0L
    private var blockId = 0L

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

        val userId = database.userDao().insertUser(
            UserEntity(
                email = "harvest-integration@example.com",
                passwordHash = "test-hash",
                passwordSalt = "test-salt"
            )
        )

        vineyardId = database.vineyardDao().insertVineyard(
            VineyardEntity(
                userId = userId,
                name = "Harvest Test Vineyard",
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

        database.blockDao().upsertBlock(
            BlockEntity(
                blockId = 1L,
                vineyardId = vineyardId,
                name = "Block A",
                area = 100,
                vineVariety = "Merlot",
                color = "Red",
                rootStock = "SO4",
                rowCount = 10,
                rowSpacing = 2,
                plantedAt = LocalDate(2020, 4, 1),
                phenologicalStage = null
            )
        )

        blockId = 1L
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun harvestStoresAndRetrievesDecimalValues(): Unit = runBlocking {
        val harvestId = database.harvestDao().insertHarvest(
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

        val storedHarvest = database.harvestDao()
            .getHarvestForBlock(
                blockId = blockId,
                harvestId = harvestId
            )

        assertNotNull(storedHarvest)

        assertEquals(harvestId, storedHarvest.harvestId)
        assertEquals(blockId, storedHarvest.blockId)
        assertEquals("2026", storedHarvest.season)
        assertEquals(testDate, storedHarvest.harvestDate)
        assertEquals("kg", storedHarvest.units)
        assertEquals(1250.75, storedHarvest.quantity)
        assertEquals(22.5, storedHarvest.brixSugarContent)
    }


    @Test
    fun observeHarvestsForBlockReturnsOnlyMatchingBlock(): Unit = runBlocking {
        val secondBlockId = 2L

        database.blockDao().upsertBlock(
            BlockEntity(
                blockId = secondBlockId,
                vineyardId = vineyardId,
                name = "Block B",
                area = 80,
                vineVariety = "Cabernet Sauvignon",
                color = "Red",
                rootStock = "SO4",
                rowCount = 8,
                rowSpacing = 2,
                plantedAt = LocalDate(2021, 4, 1),
                phenologicalStage = null
            )
        )

        val firstHarvestId = database.harvestDao().insertHarvest(
            HarvestEntity(
                harvestId = 0L,
                blockId = blockId,
                season = "2026",
                harvestDate = LocalDate(2026, 9, 20),
                units = "kg",
                quantity = 1250.75,
                brixSugarContent = 22.5
            )
        )

        val secondHarvestId = database.harvestDao().insertHarvest(
            HarvestEntity(
                harvestId = 0L,
                blockId = secondBlockId,
                season = "2026",
                harvestDate = LocalDate(2026, 10, 1),
                units = "kg",
                quantity = 950.25,
                brixSugarContent = 24.0
            )
        )

        val firstBlockHarvests = database.harvestDao()
            .observeHarvestsForBlock(blockId)
            .first()

        val secondBlockHarvests = database.harvestDao()
            .observeHarvestsForBlock(secondBlockId)
            .first()

        assertEquals(1, firstBlockHarvests.size)
        assertEquals(firstHarvestId, firstBlockHarvests.single().harvestId)
        assertEquals(1250.75, firstBlockHarvests.single().quantity)

        assertEquals(1, secondBlockHarvests.size)
        assertEquals(secondHarvestId, secondBlockHarvests.single().harvestId)
        assertEquals(950.25, secondBlockHarvests.single().quantity)
    }


    @Test
    fun observeHarvestsForBlockEmitsAfterInsertion(): Unit = runBlocking {
        val emissions = mutableListOf<List<HarvestEntity>>()

        val collectionJob = launch {
            database.harvestDao()
                .observeHarvestsForBlock(blockId)
                .take(2)
                .toList(emissions)
        }

        // Wait until the Flow collector has started and received
        // the initial empty list.
        withTimeout(5_000) {
            while (emissions.isEmpty()) {
                yield()
            }
        }

        assertEquals(emptyList(), emissions.first())

        val harvestId = database.harvestDao().insertHarvest(
            HarvestEntity(
                harvestId = 0L,
                blockId = blockId,
                season = "2026",
                harvestDate = LocalDate(2026, 10, 1),
                units = "kg",
                quantity = 1250.75,
                brixSugarContent = 22.5
            )
        )

        withTimeout(5_000) {
            collectionJob.join()
        }

        assertEquals(2, emissions.size)

        val updatedHarvests = emissions[1]

        assertEquals(1, updatedHarvests.size)
        assertEquals(harvestId, updatedHarvests.single().harvestId)
        assertEquals(1250.75, updatedHarvests.single().quantity)
        assertEquals(22.5, updatedHarvests.single().brixSugarContent)
    }



    @Test
    fun updateHarvestUpdatesExistingRecord(): Unit = runBlocking {
        val harvestId = database.harvestDao().insertHarvest(
            HarvestEntity(
                harvestId = 0L,
                blockId = blockId,
                season = "2026",
                harvestDate = LocalDate(2026, 9, 20),
                units = "kg",
                quantity = 1250.75,
                brixSugarContent = 22.5
            )
        )

        val updatedRows = database.harvestDao().updateHarvest(
            harvestId = harvestId,
            blockId = blockId,
            season = "2026",
            harvestDate = LocalDate(2026, 10, 5),
            units = "kg",
            quantity = 1450.25,
            brixSugarContent = 23.75
        )

        assertEquals(1, updatedRows)

        val updatedHarvest = database.harvestDao()
            .getHarvestForBlock(blockId, harvestId)

        assertNotNull(updatedHarvest)

        assertEquals(harvestId, updatedHarvest.harvestId)
        assertEquals(blockId, updatedHarvest.blockId)
        assertEquals("2026", updatedHarvest.season)
        assertEquals(LocalDate(2026, 10, 5), updatedHarvest.harvestDate)
        assertEquals("kg", updatedHarvest.units)
        assertEquals(1450.25, updatedHarvest.quantity)
        assertEquals(23.75, updatedHarvest.brixSugarContent)
    }


    @Test
    fun updateHarvestWithWrongBlockIdDoesNotModifyRecord(): Unit = runBlocking {
        val harvestId = database.harvestDao().insertHarvest(
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

        val wrongBlockId = blockId + 999L

        val updatedRows = database.harvestDao().updateHarvest(
            harvestId = harvestId,
            blockId = wrongBlockId,
            season = "2027",
            harvestDate = LocalDate(2027, 10, 1),
            units = "tons",
            quantity = 9999.99,
            brixSugarContent = 30.0
        )

        assertEquals(0, updatedRows)

        val originalHarvest = database.harvestDao()
            .getHarvestForBlock(blockId, harvestId)

        assertNotNull(originalHarvest)

        assertEquals("2026", originalHarvest.season)
        assertEquals(testDate, originalHarvest.harvestDate)
        assertEquals("kg", originalHarvest.units)
        assertEquals(1250.75, originalHarvest.quantity)
        assertEquals(22.5, originalHarvest.brixSugarContent)
    }



    @Test
    fun updateNonexistentHarvestReturnsZeroRows(): Unit = runBlocking {
        val updatedRows = database.harvestDao().updateHarvest(
            harvestId = 99999L,
            blockId = blockId,
            season = "2026",
            harvestDate = testDate,
            units = "kg",
            quantity = 1000.5,
            brixSugarContent = 21.0
        )

        assertEquals(0, updatedRows)

        val harvests = database.harvestDao()
            .observeHarvestsForBlock(blockId)
            .first()

        assertEquals(emptyList(), harvests)
    }


    @Test
    fun deleteHarvestRemovesOnlyRequestedRecord(): Unit = runBlocking {
        val firstHarvestId = database.harvestDao().insertHarvest(
            HarvestEntity(
                harvestId = 0L,
                blockId = blockId,
                season = "2026",
                harvestDate = LocalDate(2026, 9, 20),
                units = "kg",
                quantity = 1250.75,
                brixSugarContent = 22.5
            )
        )

        val secondHarvestId = database.harvestDao().insertHarvest(
            HarvestEntity(
                harvestId = 0L,
                blockId = blockId,
                season = "2026",
                harvestDate = LocalDate(2026, 10, 1),
                units = "kg",
                quantity = 950.25,
                brixSugarContent = 23.5
            )
        )

        val deletedRows = database.harvestDao().deleteHarvest(
            blockId = blockId,
            harvestId = firstHarvestId
        )

        assertEquals(1, deletedRows)

        val deletedHarvest = database.harvestDao()
            .getHarvestForBlock(blockId, firstHarvestId)

        assertEquals(null, deletedHarvest)

        val remainingHarvest = database.harvestDao()
            .getHarvestForBlock(blockId, secondHarvestId)

        assertNotNull(remainingHarvest)
        assertEquals(950.25, remainingHarvest.quantity)

        val remainingCount = database.harvestDao()
            .countHarvestsForBlock(blockId)

        assertEquals(1, remainingCount)
    }



    @Test
    fun deleteHarvestWithWrongBlockIdDoesNotDeleteRecord(): Unit = runBlocking {
        val harvestId = database.harvestDao().insertHarvest(
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

        val wrongBlockId = blockId + 999L

        val deletedRows = database.harvestDao().deleteHarvest(
            blockId = wrongBlockId,
            harvestId = harvestId
        )

        assertEquals(0, deletedRows)

        val originalHarvest = database.harvestDao()
            .getHarvestForBlock(blockId, harvestId)

        assertNotNull(originalHarvest)
        assertEquals(1250.75, originalHarvest.quantity)
        assertEquals(22.5, originalHarvest.brixSugarContent)

        assertEquals(
            1,
            database.harvestDao().countHarvestsForBlock(blockId)
        )
    }


    @Test
    fun deleteNonexistentHarvestReturnsZeroRows(): Unit = runBlocking {
        val deletedRows = database.harvestDao().deleteHarvest(
            blockId = blockId,
            harvestId = 99999L
        )

        assertEquals(0, deletedRows)

        val remainingCount = database.harvestDao()
            .countHarvestsForBlock(blockId)

        assertEquals(0, remainingCount)
    }


    @Test
    fun repositoryCreatesAndRetrievesHarvest(): Unit = runBlocking {
        val repository = createHarvestRepository()

        val harvest = HarvestDomainModel(
            harvestId = 0L,
            blockId = blockId,
            season = "2026",
            harvestDate = LocalDate(2026, 10, 1),
            units = "kg",
            quantity = 1250.75,
            brixSugarContent = 22.5
        )

        val createdId = repository.createHarvest(harvest)

        assertNotNull(createdId)
        assertTrue(createdId > 0L)

        val retrievedHarvest = repository.getHarvest(
            blockId = blockId,
            harvestId = createdId
        )

        assertNotNull(retrievedHarvest)

        assertEquals(createdId, retrievedHarvest.harvestId)
        assertEquals(blockId, retrievedHarvest.blockId)
        assertEquals("2026", retrievedHarvest.season)
        assertEquals(LocalDate(2026, 10, 1), retrievedHarvest.harvestDate)
        assertEquals("kg", retrievedHarvest.units)
        assertEquals(1250.75, retrievedHarvest.quantity)
        assertEquals(22.5, retrievedHarvest.brixSugarContent)
    }


    @Test
    fun repositoryDoesNotRetrieveHarvestFromWrongBlock(): Unit = runBlocking {
        val repository = createHarvestRepository()

        val harvest = HarvestDomainModel(
            harvestId = 0L,
            blockId = blockId,
            season = "2026",
            harvestDate = testDate,
            units = "kg",
            quantity = 950.25,
            brixSugarContent = 23.5
        )

        val createdId = repository.createHarvest(harvest)

        assertNotNull(createdId)

        val result = repository.getHarvest(
            blockId = blockId + 999L,
            harvestId = createdId
        )

        assertNull(result)

        // Confirm that the harvest still exists in its actual block.
        val originalHarvest = repository.getHarvest(
            blockId = blockId,
            harvestId = createdId
        )

        assertNotNull(originalHarvest)
        assertEquals(950.25, originalHarvest.quantity)
    }


    @Test
    fun repositoryObservesHarvestsForBlock(): Unit = runBlocking {
        val repository = createHarvestRepository()

        val firstId = repository.createHarvest(
            HarvestDomainModel(
                harvestId = 0L,
                blockId = blockId,
                season = "2026",
                harvestDate = LocalDate(2026, 9, 20),
                units = "kg",
                quantity = 1250.75,
                brixSugarContent = 22.5
            )
        )

        val secondId = repository.createHarvest(
            HarvestDomainModel(
                harvestId = 0L,
                blockId = blockId,
                season = "2026",
                harvestDate = LocalDate(2026, 10, 1),
                units = "kg",
                quantity = 950.25,
                brixSugarContent = 23.5
            )
        )

        assertNotNull(firstId)
        assertNotNull(secondId)

        val harvests = repository
            .observeHarvestsForBlock(blockId)
            .first()

        assertEquals(2, harvests.size)

        // The DAO orders by harvest date descending.
        assertEquals(secondId, harvests[0].harvestId)
        assertEquals(firstId, harvests[1].harvestId)

        assertEquals(950.25, harvests[0].quantity)
        assertEquals(23.5, harvests[0].brixSugarContent)

        assertEquals(1250.75, harvests[1].quantity)
        assertEquals(22.5, harvests[1].brixSugarContent)
    }


    @Test
    fun repositoryUpdatesHarvestSuccessfully(): Unit = runBlocking {
        val repository = createHarvestRepository()

        val createdId = repository.createHarvest(
            HarvestDomainModel(
                harvestId = 0L,
                blockId = blockId,
                season = "2026",
                harvestDate = testDate,
                units = "kg",
                quantity = 1250.75,
                brixSugarContent = 22.5
            )
        )

        assertNotNull(createdId)

        val updated = repository.updateHarvest(
            HarvestDomainModel(
                harvestId = createdId,
                blockId = blockId,
                season = "2027",
                harvestDate = LocalDate(2027, 10, 5),
                units = "kg",
                quantity = 1450.25,
                brixSugarContent = 24.75
            )
        )

        assertTrue(updated)

        val storedHarvest = repository.getHarvest(
            blockId = blockId,
            harvestId = createdId
        )

        assertNotNull(storedHarvest)

        assertEquals("2027", storedHarvest.season)
        assertEquals(LocalDate(2027, 10, 5), storedHarvest.harvestDate)
        assertEquals(1450.25, storedHarvest.quantity)
        assertEquals(24.75, storedHarvest.brixSugarContent)
    }


    @Test
    fun repositoryDeletesHarvestSuccessfully(): Unit = runBlocking {
        val repository = createHarvestRepository()

        val createdId = repository.createHarvest(
            HarvestDomainModel(
                harvestId = 0L,
                blockId = blockId,
                season = "2026",
                harvestDate = testDate,
                units = "kg",
                quantity = 1250.75,
                brixSugarContent = 22.5
            )
        )

        assertNotNull(createdId)

        val deleted = repository.deleteHarvest(
            blockId = blockId,
            harvestId = createdId
        )

        assertTrue(deleted)

        val storedHarvest = repository.getHarvest(
            blockId = blockId,
            harvestId = createdId
        )

        assertNull(storedHarvest)

        assertEquals(
            0,
            database.harvestDao().countHarvestsForBlock(blockId)
        )
    }


    @Test
    fun repositoryUpdateWithWrongBlockIdDoesNotModifyHarvest(): Unit = runBlocking {
        val repository = createHarvestRepository()

        val createdId = repository.createHarvest(
            HarvestDomainModel(
                harvestId = 0L,
                blockId = blockId,
                season = "2026",
                harvestDate = testDate,
                units = "kg",
                quantity = 1250.75,
                brixSugarContent = 22.5
            )
        )

        assertNotNull(createdId)

        val updated = repository.updateHarvest(
            HarvestDomainModel(
                harvestId = createdId,
                blockId = blockId + 999L,
                season = "2027",
                harvestDate = LocalDate(2027, 10, 1),
                units = "tons",
                quantity = 9999.99,
                brixSugarContent = 30.0
            )
        )

        assertFalse(updated)

        val originalHarvest = repository.getHarvest(
            blockId = blockId,
            harvestId = createdId
        )

        assertNotNull(originalHarvest)

        assertEquals("2026", originalHarvest.season)
        assertEquals(testDate, originalHarvest.harvestDate)
        assertEquals("kg", originalHarvest.units)
        assertEquals(1250.75, originalHarvest.quantity)
        assertEquals(22.5, originalHarvest.brixSugarContent)
    }


    @Test
    fun repositoryDeleteWithWrongBlockIdDoesNotDeleteHarvest(): Unit = runBlocking {
        val repository = createHarvestRepository()

        val createdId = repository.createHarvest(
            HarvestDomainModel(
                harvestId = 0L,
                blockId = blockId,
                season = "2026",
                harvestDate = testDate,
                units = "kg",
                quantity = 1250.75,
                brixSugarContent = 22.5
            )
        )

        assertNotNull(createdId)

        val deleted = repository.deleteHarvest(
            blockId = blockId + 999L,
            harvestId = createdId
        )

        assertFalse(deleted)

        val originalHarvest = repository.getHarvest(
            blockId = blockId,
            harvestId = createdId
        )

        assertNotNull(originalHarvest)

        assertEquals(createdId, originalHarvest.harvestId)
        assertEquals(1250.75, originalHarvest.quantity)

        assertEquals(
            1,
            database.harvestDao().countHarvestsForBlock(blockId)
        )
    }



    private fun createHarvestRepository() = HarvestRepositoryImpl(
        harvestDao = database.harvestDao(),
        harvestMapper = HarvestMapper()
    )

}
