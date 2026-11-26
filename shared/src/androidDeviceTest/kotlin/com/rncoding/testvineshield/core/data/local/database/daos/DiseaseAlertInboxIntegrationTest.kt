package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.entities.*
import com.rncoding.testvineshield.core.data.local.database.projections.DiseaseAlertInboxRow
import com.rncoding.testvineshield.core.domain.datamodels.enums.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class DiseaseAlertInboxIntegrationTest {
    @Test fun ownedInboxAndCompareBeforeReviewDoNotLeakOrOverwrite(): Unit = runBlocking {
        withTimeout(30.seconds) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val db = Room.inMemoryDatabaseBuilder<VineshieldDatabase>(context).setDriver(BundledSQLiteDriver()).build()
            try {
                val date = LocalDate(2026, 11, 25)
                val owner = db.userDao().insertUser(UserEntity(email = "alert-owner@example.invalid", passwordHash = "fixture", passwordSalt = "fixture"))
                val other = db.userDao().insertUser(UserEntity(email = "alert-other@example.invalid", passwordHash = "fixture", passwordSalt = "fixture"))
                suspend fun vineyard(id: Long, label: String) = db.vineyardDao().insertVineyard(VineyardEntity(userId = id,
                    name = label, size = 1.0, country = "BG", city = "Sofia", latitude = 42.7, longitude = 23.3,
                    timeZone = "Europe/Sofia", elevation = null, sortOrder = 0, createdAt = date, updatedAt = date))
                val firstVineyard = vineyard(owner, "First")
                val otherVineyard = vineyard(other, "Private other")
                val disease = db.diseaseDao().insertPredefinedDiseases(listOf(DiseaseEntity(0, "fixture-alert", "Fixture only", "", "", "", "", true))).single()
                val dao = db.diseaseAlertDao()
                val header = DiseaseAlertEntity(100, disease, firstVineyard, null, date, "Synthetic alert — not model evidence", AlertStatus.ACTIVE, AlertSeverity.HIGH, PhenologicalStage.FLOWERING)
                dao.upsertDiseaseAlert(header)
                dao.upsertDiseaseAlert(header.copy(alertId = 101, vineyardId = otherVineyard, alert = "Other owner"))
                var viewed = dao.observeOwnedInbox(owner).first().single()
                assertEquals(100L, viewed.alertId); assertEquals("First", viewed.vineyardName)
                assertEquals(listOf(101L), dao.observeOwnedInbox(other).first().map { it.alertId })
                suspend fun review(user: Long, row: DiseaseAlertInboxRow) = dao.acknowledgeOwned(user, row.vineyardId,
                    row.alertId, row.diseaseId, row.blockId, row.createdAt, row.message, row.severity, row.stage)
                assertEquals(0, review(other, viewed))
                // Concurrent invalidation wins over review.
                dao.upsertDiseaseAlert(header.copy(alertStatus = AlertStatus.INVALIDATED))
                assertEquals(0, review(owner, viewed))
                assertEquals(AlertStatus.INVALIDATED, dao.observeOwnedInbox(owner).first().single().status)
                // A changed message is also stale, even when its date and active status are unchanged.
                dao.upsertDiseaseAlert(header.copy(alert = "New saved context"))
                assertEquals(0, review(owner, viewed))
                viewed = dao.observeOwnedInbox(owner).first().single()
                assertEquals(1, review(owner, viewed))
                assertEquals(0, review(owner, viewed))
                assertEquals(AlertStatus.ACKNOWLEDGED, dao.observeOwnedInbox(owner).first().single().status)
                assertEquals(AlertStatus.ACTIVE, dao.observeOwnedInbox(other).first().single().status)
                db.vineyardDao().deleteVineyard(owner, firstVineyard)
                assertTrue(dao.observeOwnedInbox(owner).first().isEmpty())
                assertEquals(1, dao.observeOwnedInbox(other).first().size)
            } finally { db.close() }
        }
    }
}
