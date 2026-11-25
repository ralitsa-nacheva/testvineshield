package com.rncoding.testvineshield.core.data.local.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.entities.*
import com.rncoding.testvineshield.core.domain.datamodels.SprayWriteOutcome
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/** Real Room graph and reopen check; not UI/authentication/encryption/backup certification. */
@RunWith(AndroidJUnit4::class)
class NotebookPersistenceJourneyTest {
    @Test fun notebookGraphSurvivesReopenAndOwnedDeletionIsScoped(): Unit = runBlocking {
        withTimeout(30.seconds) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            // Unique test database: never open or delete the user's vineshield.db.
            val name = "notebook-journey-${UUID.randomUUID()}.db"
            fun open() = buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context, name))
            var db = open()
            try {
                val date = LocalDate(2026, 11, 25)
                val owner = db.userDao().insertUser(UserEntity(email = "demo-owner@example.invalid", passwordHash = "fixture-hash", passwordSalt = "fixture-salt"))
                val other = db.userDao().insertUser(UserEntity(email = "demo-other@example.invalid", passwordHash = "fixture-hash", passwordSalt = "fixture-salt"))
                suspend fun vineyard(userId: Long, label: String) = db.vineyardDao().insertVineyard(VineyardEntity(
                    userId = userId, name = label, size = 1.25, country = "Bulgaria", city = "Plovdiv",
                    latitude = 42.1, longitude = 24.7, timeZone = "Europe/Sofia", elevation = 200.5,
                    sortOrder = 0, createdAt = date, updatedAt = date))
                val vineyardId = vineyard(owner, "Demo Лозе")
                val otherVineyard = vineyard(other, "Other owner")
                val blockId = db.blockDao().insertBlock(BlockEntity(
                    blockId = 0, vineyardId = vineyardId, name = "North", area = null, vineVariety = "Local",
                    color = "", rootStock = "", rowCount = null, rowSpacing = null, plantedAt = null,
                    phenologicalStage = null))
                val activityId = db.activityDao().insertActivity(ActivityEntity(
                    activityId = 0, vineyardId = vineyardId, blockId = blockId, activityType = "Demo spray",
                    activityDescription = "Synthetic record only", season = "2026", phenologicalStage = null,
                    priority = ActivityPriority.MEDIUM, status = ActivityStatus.COMPLETED, plannedAt = date,
                    completedAt = date, tool = null, notes = "Бележка / note", createdAt = 1, updatedAt = 1,
                    treatmentProduct = null, treatmentProductQuantity = null, treatmentProductUnits = null,
                    waterQuantity = null, treatmentProductRate = null))
                val header = SprayApplicationEntity(activityId, date, null, "Europe/Sofia", null, null, null, null, "Synthetic", 0, "")
                val product = SprayProductEntity(0, activityId, 0, "Demo product — not treatment advice", null, null, null)
                assertEquals(SprayWriteOutcome.SAVED, db.sprayDao().replace(vineyardId, header, listOf(product)))
                val savedSpray = assertNotNull(db.sprayDao().get(vineyardId, activityId))
                val harvestId = db.harvestDao().insertHarvest(HarvestEntity(0, blockId, "2026", date, "kg", 12.5, 0.0))
                // Close/reopen exercises file persistence, not a simulated in-memory restart.
                db.close()
                db = open()
                assertEquals("Demo Лозе", assertNotNull(db.vineyardDao().getVineyard(owner, vineyardId)).name)
                assertEquals(200.5, assertNotNull(db.vineyardDao().getVineyard(owner, vineyardId)).elevation)
                assertNull(db.vineyardDao().getVineyard(other, vineyardId))
                assertNotNull(db.blockDao().getBlockById(vineyardId, blockId))
                assertEquals("Бележка / note", assertNotNull(db.activityDao().getActivityById(vineyardId, activityId)).notes)
                assertEquals(savedSpray, db.sprayDao().get(vineyardId, activityId))
                val harvest = assertNotNull(db.harvestDao().getHarvestForBlock(blockId, harvestId))
                assertEquals(12.5, harvest.quantity); assertEquals("kg", harvest.units); assertEquals(0.0, harvest.brixSugarContent)
                // Stale header cannot overwrite the version/generation saved before reopening.
                assertEquals(SprayWriteOutcome.CONFLICT, db.sprayDao().replace(vineyardId, header, listOf(product)))
                assertEquals(savedSpray, db.sprayDao().get(vineyardId, activityId))
                assertEquals(0, db.vineyardDao().deleteVineyard(other, vineyardId))
                assertNotNull(db.vineyardDao().getVineyard(owner, vineyardId))
                assertEquals(1, db.vineyardDao().deleteVineyard(owner, vineyardId))
                assertNull(db.blockDao().getBlockById(vineyardId, blockId))
                assertNull(db.activityDao().getActivityById(vineyardId, activityId))
                assertNull(db.sprayDao().get(vineyardId, activityId))
                assertTrue(db.sprayDao().lines(activityId).isEmpty())
                assertNull(db.harvestDao().getHarvestForBlock(blockId, harvestId))
                assertNotNull(db.vineyardDao().getVineyard(other, otherVineyard))
            } finally {
                db.close()
                context.deleteDatabase(name)
            }
        }
    }
}
