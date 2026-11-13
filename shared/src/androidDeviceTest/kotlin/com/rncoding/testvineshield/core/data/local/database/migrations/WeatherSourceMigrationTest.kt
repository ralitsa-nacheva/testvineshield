package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.*
import com.rncoding.testvineshield.core.data.local.mappers.*
import com.rncoding.testvineshield.core.data.remote.WeatherResponseFixtures
import com.rncoding.testvineshield.core.data.local.database.seed.DiseaseCatalogueSeeder
import com.rncoding.testvineshield.core.domain.disease_risk.MonitoringDraftSaveOutcome
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.entities.ActivityEntity
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class WeatherSourceMigrationTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun raw(name: String,action: (SQLiteConnection)->Unit) {
        context.getDatabasePath(name).parentFile?.mkdirs()
        val c=BundledSQLiteDriver().open(context.getDatabasePath(name).absolutePath)
        try { action(c) } finally { c.close() }
    }
    private fun seed(name: String) { raw(name) { c ->
        SchemaV2Fixture.create(c); MIGRATION_2_3.migrate(c); MIGRATION_3_4.migrate(c); MIGRATION_4_5.migrate(c)
        MIGRATION_5_6.migrate(c); MIGRATION_6_7.migrate(c); MIGRATION_7_8.migrate(c); MIGRATION_8_9.migrate(c)
        c.execSQL("PRAGMA user_version=9")
    } }
    private fun open(name: String)=buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context,name))
    private fun snapshot(c: SQLiteConnection): Map<String,List<List<String>>> {
        val names=mutableListOf<String>(); val q=c.prepare("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name!='room_master_table' AND name!='block_stage_observation' AND name NOT LIKE 'weather_source' AND name NOT LIKE 'weather_history_source' AND name NOT LIKE 'weather_cache_status' AND name NOT LIKE 'weather_history_archive' ORDER BY name")
        try { while(q.step()) names.add(q.getText(0)) } finally { q.close() }
        return names.associateWith { table ->
            val fields=mutableListOf<String>(); val info=c.prepare("PRAGMA table_info(`$table`)")
            try { while(info.step()) {
                val field = info.getText(1)
                if (table != "block" || field != "stage_observed_on") fields.add(field)
            } } finally { info.close() }
            val rows=c.prepare("SELECT "+fields.joinToString { "quote(`$it`)" }+" FROM `$table`")
            try { buildList { while(rows.step()) add(fields.indices.map { rows.getText(it) }) }.sortedBy { it.toString() } } finally { rows.close() }
        }
    }
    private suspend fun move(db: VineshieldDatabase,latitude: Double) {
        val v=assertNotNull(db.vineyardDao().getVineyard(1,10))
        assertEquals(1,db.vineyardDao().updateVineyard(10,1,v.name,v.size,v.country,v.city,latitude,v.longitude,v.timeZone,v.elevation,v.updatedAt,v.sizeUnit))
    }
    @Test fun installedUpgradePreservesAllOldTablesAndDoesNotGuessLegacyProvenance(): Unit=runBlocking {
        val name="r06b2-preserve-${UUID.randomUUID()}.db"; seed(name); var before: Map<String,List<List<String>>>?=null
        raw(name) { before=snapshot(it) }; val db=open(name)
        try {
            assertNotNull(db.weatherCacheDao().generation(10))
            assertTrue(db.weatherCacheDao().observeHistory(10,0).first().isEmpty())
            assertNotNull(db.weatherHistoryDao().getLatestWeatherByVineyard(10))
            db.close(); raw(name) { assertEquals(before,snapshot(it)) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun schemaTenUpgradeKeepsLegacyStageAndPersistsAnExplicitDate(): Unit = runBlocking {
        val name = "r07a-stage-${UUID.randomUUID()}.db"
        seed(name)
        raw(name) { connection ->
            MIGRATION_9_10.migrate(connection)
            connection.execSQL("UPDATE block SET phenological_stage = 'flowering' WHERE block_id = 20")
            connection.execSQL("PRAGMA user_version=10")
        }
        val date = kotlinx.datetime.LocalDate(2024, 5, 17)
        val db = open(name)
        try {
            val dao = db.blockDao()
            val original = assertNotNull(dao.getBlockById(10, 20))
            assertEquals(com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage.FLOWERING, original.phenologicalStage)
            assertNull(original.stageObservedOn)
            val recorded = original.copy(stageObservedOn = date)
            val repo = com.rncoding.testvineshield.core.data.repository.BlockRepositoryImpl(dao, BlockMapper())
            assertTrue(repo.updateBlock(BlockMapper().toDomain(recorded)))
            assertEquals(recorded, dao.getBlockById(10, 20))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun stageHistorySeedsDatedRowsAndSurvivesLaterEdits(): Unit = runBlocking {
        val name = "r07c-stage-history-${UUID.randomUUID()}.db"
        val firstDate = kotlinx.datetime.LocalDate(2024, 5, 17)
        val secondDate = kotlinx.datetime.LocalDate(2024, 6, 1)
        val flowering = com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage.FLOWERING
        val harvest = com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage.HARVEST
        seed(name)
        raw(name) { connection ->
            MIGRATION_9_10.migrate(connection)
            MIGRATION_10_11.migrate(connection)
            connection.execSQL("UPDATE block SET phenological_stage='flowering', stage_observed_on='2024-05-17' WHERE block_id=20")
            connection.execSQL("PRAGMA user_version=11")
        }
        val db = open(name)
        try {
            val dao = db.blockDao()
            val mapper = BlockMapper()
            val repo = com.rncoding.testvineshield.core.data.repository.BlockRepositoryImpl(dao, mapper)
            val original = mapper.toDomain(assertNotNull(dao.getBlockById(10, 20)))
            assertEquals(flowering, dao.getStageObservation(10, 20, firstDate)?.stage)
            assertTrue(repo.updateBlock(original.copy(phenologicalStage = harvest, stageObservedOn = secondDate)))
            assertEquals(listOf(secondDate, firstDate), repo.getRecentStageObservations(1, 10, 20).map { it.observedOn })
            assertTrue(repo.getRecentStageObservations(999, 10, 20).isEmpty())

            val provider = com.rncoding.testvineshield.core.data.disease_risk.DefaultDiseaseRiskHostContextProvider(
                dao, db.diseaseOccurrenceDao()
            )
            assertEquals(flowering, provider.getHostContext(10, 20, 1, firstDate).phenologicalStage)
            assertNull(provider.getHostContext(10, 20, 1, kotlinx.datetime.LocalDate(2024, 5, 18)).phenologicalStage)
            assertTrue(repo.updateBlock(original.copy(phenologicalStage = null, stageObservedOn = null)))
            assertEquals(harvest, dao.getStageObservation(10, 20, secondDate)?.stage)
            assertTrue(repo.deleteBlock(10, 20))
            assertTrue(repo.getRecentStageObservations(1, 10, 20).isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
        val freshName = "r07c-stage-fresh-${UUID.randomUUID()}.db"
        val fresh = open(freshName)
        try {
            fresh.blockDao().getRecentStageObservations(1, 10, 20)
            fresh.close()
            raw(freshName) { connection ->
                val query = connection.prepare(
                    "SELECT count(*) FROM sqlite_master WHERE type='trigger' AND name LIKE 'block_stage_history_%'"
                )
                try {
                    assertTrue(query.step())
                    assertEquals(2L, query.getLong(0))
                } finally {
                    query.close()
                }
            }
        } finally {
            fresh.close()
            context.deleteDatabase(freshName)
        }
    }

    @Test fun botrytisDraftIsOwnerScopedAndNeverOverwritesAnEnabledConfiguration(): Unit = runBlocking {
        val name = "r07h-monitoring-draft-${UUID.randomUUID()}.db"
        seed(name)
        val db = open(name)
        try {
            val dao = db.diseaseRiskMonitoringConfigurationDao()
            val code = "grape_botrytis"
            val model = "BOTRYTIS"
            assertEquals(MonitoringDraftSaveOutcome.DISEASE_UNAVAILABLE,
                dao.saveDisabledDraft(1, 10, 20, "missing_catalogue_code", model, "2026-06-15"))
            DiseaseCatalogueSeeder(db).seed()
            val diseaseId = assertNotNull(dao.catalogueDiseaseId(code))
            assertEquals(MonitoringDraftSaveOutcome.BLOCK_UNAVAILABLE,
                dao.saveDisabledDraft(2, 10, 20, code, model, "2026-06-15"))
            assertEquals(MonitoringDraftSaveOutcome.BLOCK_UNAVAILABLE,
                dao.saveDisabledDraft(1, 11, 20, code, model, "2026-06-15"))
            assertTrue(dao.observeConfigurationsForVineyard(10).first().isEmpty())
            assertEquals(MonitoringDraftSaveOutcome.SAVED,
                dao.saveDisabledDraft(1, 10, 20, code, model, "2026-06-15"))
            assertEquals(MonitoringDraftSaveOutcome.SAVED,
                dao.saveDisabledDraft(1, 10, 20, code, model, "2026-06-16"))
            val saved = assertNotNull(dao.getConfiguration(10, 20, diseaseId, model))
            assertFalse(saved.enabled)
            assertEquals("2026-06-16", saved.monitoringStartDate)
            assertTrue(dao.getEnabledConfigurationsForVineyard(10).isEmpty())
            dao.upsertConfiguration(saved.copy(enabled = true))
            assertEquals(MonitoringDraftSaveOutcome.ALREADY_ENABLED,
                dao.saveDisabledDraft(1, 10, 20, code, model, "2026-06-17"))
            assertEquals(saved.copy(enabled = true), dao.getConfiguration(10, 20, diseaseId, model))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun diseaseObservationWriteAndReadRequireMatchingOwnerAndBlock(): Unit = runBlocking {
        val name = "r09c-observation-${UUID.randomUUID()}.db"
        seed(name)
        val db = open(name)
        try {
            DiseaseCatalogueSeeder(db).seed()
            val disease = assertNotNull(db.diseaseDao().getDiseaseByCode("grape_botrytis"))
            val dao = db.diseaseOccurrenceDao()
            val occurrence = DiseaseOccurrenceEntity(
                occurrenceId = 0, diseaseId = disease.diseaseId, vineyardId = 10, blockId = 20,
                observedAt = LocalDate(2026, 6, 15), severity = 12,
                status = DiseaseOccurrenceStatus.ACTIVE, curedAt = null,
                createdAt = 1000, updatedAt = 1000, notes = "Near the row end"
            )
            assertNull(dao.insertOwnedBlockOccurrence(2, occurrence))
            assertNull(dao.insertOwnedBlockOccurrence(1, occurrence.copy(vineyardId = 11)))
            assertNull(dao.insertOwnedBlockOccurrence(1, occurrence.copy(diseaseId = 0)))
            val savedId = assertNotNull(dao.insertOwnedBlockOccurrence(1, occurrence))
            val saved = dao.observeOwnedBlockOccurrences(1, 10, 20).first().single()
            assertEquals(savedId, saved.id)
            assertEquals(occurrence.notes, saved.notes)
            assertEquals(occurrence.observedAt, saved.observedOn)
            val active = DiseaseOccurrenceStatus.ACTIVE
            val monitoring = DiseaseOccurrenceStatus.MONITORING
            val resolved = DiseaseOccurrenceStatus.RESOLVED
            val resolutionDate = LocalDate.parse("2026-11-12")
            assertEquals(0, dao.updateOwnedBlockOccurrenceStatus(
                2, 10, 20, savedId, saved.updatedAt, resolved, resolutionDate, saved.updatedAt
            ))
            assertEquals(0, dao.updateOwnedBlockOccurrenceStatus(
                1, 11, 20, savedId, saved.updatedAt, resolved, resolutionDate, saved.updatedAt
            ))
            assertEquals(1, dao.updateOwnedBlockOccurrenceStatus(
                1, 10, 20, savedId, saved.updatedAt, resolved, resolutionDate, saved.updatedAt
            ))
            assertEquals(0, dao.updateOwnedBlockOccurrenceStatus(
                1, 10, 20, savedId, saved.updatedAt, monitoring, null, saved.updatedAt
            ))
            val closed = dao.observeOwnedBlockOccurrences(1, 10, 20).first().single()
            assertEquals(resolved, closed.status)
            assertEquals(resolutionDate, closed.curedAt)
            assertTrue(closed.updatedAt > saved.updatedAt)
            assertEquals(0, dao.updateOwnedBlockOccurrenceDetails(
                1, 10, 20, savedId, saved.updatedAt, closed.observedOn, 30, null, saved.updatedAt
            ))
            assertEquals(0, dao.updateOwnedBlockOccurrenceDetails(
                1, 10, 20, savedId, closed.updatedAt, LocalDate(2026, 11, 13), 30, null, closed.updatedAt
            ))
            assertEquals(1, dao.updateOwnedBlockOccurrenceStatus(
                1, 10, 20, savedId, closed.updatedAt, monitoring, null, closed.updatedAt
            ))
            val underReview = dao.observeOwnedBlockOccurrences(1, 10, 20).first().single()
            assertEquals(monitoring, underReview.status)
            assertNull(underReview.curedAt)
            assertEquals(1, dao.updateOwnedBlockOccurrenceStatus(
                1, 10, 20, savedId, underReview.updatedAt, active, null, underReview.updatedAt
            ))
            val reopened = dao.observeOwnedBlockOccurrences(1, 10, 20).first().single()
            assertEquals(saved.copy(updatedAt = reopened.updatedAt), reopened)
            val correctedDate = LocalDate(2026, 6, 14)
            assertEquals(0, dao.updateOwnedBlockOccurrenceDetails(
                2, 10, 20, savedId, reopened.updatedAt, correctedDate, 30, null, reopened.updatedAt
            ))
            assertEquals(1, dao.updateOwnedBlockOccurrenceDetails(
                1, 10, 20, savedId, reopened.updatedAt, correctedDate, 30, null, reopened.updatedAt
            ))
            val edited = dao.observeOwnedBlockOccurrences(1, 10, 20).first().single()
            assertTrue(edited.updatedAt > reopened.updatedAt)
            assertEquals(reopened.copy(observedOn = correctedDate, severity = 30, notes = null, updatedAt = edited.updatedAt), edited)
            assertEquals(0, dao.updateOwnedBlockOccurrenceStatus(
                1, 10, 20, savedId, reopened.updatedAt, resolved, resolutionDate, reopened.updatedAt
            ))
            assertEquals(occurrence.createdAt, dao.getDiseaseOccurrenceById(savedId)?.createdAt)
            assertTrue(dao.observeOwnedBlockOccurrences(2, 10, 20).first().isEmpty())
            assertTrue(dao.observeOwnedBlockOccurrences(1, 11, 20).first().isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun oilSpotRequiresOwnedDownyOccurrenceAndPreservesFirstEntry(): Unit = runBlocking {
        val name = "r09f-oil-spot-${UUID.randomUUID()}.db"
        seed(name)
        val db = open(name)
        try {
            DiseaseCatalogueSeeder(db).seed()
            val downy = assertNotNull(db.diseaseDao().getDiseaseByCode("grape_downy_mildew"))
            val botrytis = assertNotNull(db.diseaseDao().getDiseaseByCode("grape_botrytis"))
            val dao = db.diseaseOccurrenceDao()
            val observedOn = LocalDate(2026, 6, 14)
            val occurrence = DiseaseOccurrenceEntity(
                occurrenceId = 0, diseaseId = downy.diseaseId, vineyardId = 10, blockId = 20,
                observedAt = LocalDate(2026, 6, 15), severity = 12,
                status = DiseaseOccurrenceStatus.ACTIVE, curedAt = null,
                createdAt = 1000, updatedAt = 1000, notes = null
            )
            val downyId = assertNotNull(dao.insertOwnedBlockOccurrence(1, occurrence))
            val botrytisId = assertNotNull(dao.insertOwnedBlockOccurrence(1, occurrence.copy(diseaseId = botrytis.diseaseId)))
            assertFalse(dao.recordOwnedOilSpot(2, 10, 20, downyId, 1000, observedOn, "wrong owner"))
            assertFalse(dao.recordOwnedOilSpot(1, 10, 20, botrytisId, 1000, observedOn, "wrong disease"))
            assertFalse(dao.recordOwnedOilSpot(1, 10, 20, downyId, 999, observedOn, "stale"))
            assertTrue(dao.recordOwnedOilSpot(1, 10, 20, downyId, 1000, observedOn, "near the row end"))
            assertFalse(dao.recordOwnedOilSpot(1, 10, 20, downyId, 1000, observedOn, "replacement"))
            val row = dao.observeOwnedBlockOccurrences(1, 10, 20).first().single { it.id == downyId }
            assertEquals(observedOn, row.oilSpotDate)
            assertEquals("near the row end", row.oilSpotNotes)
            val evidence = dao.getRiskSymptomsForOccurrence(downyId).single()
            assertEquals("downy_mildew_oil_spot", evidence.code)
            assertEquals(observedOn, evidence.observedAt)
            val symptomId = assertNotNull(row.oilSpotId)
            val correctedDate = LocalDate(2026, 6, 13)
            assertEquals(0, dao.updateOwnedOilSpot(
                2, 10, 20, downyId, 1000, symptomId, observedOn, "near the row end", correctedDate, ""
            ))
            assertEquals(1, dao.updateOwnedOilSpot(
                1, 10, 20, downyId, 1000, symptomId, observedOn, "near the row end", correctedDate, ""
            ))
            assertEquals(0, dao.removeOwnedOilSpot(
                1, 10, 20, downyId, 1000, symptomId, observedOn, "near the row end"
            ))
            assertEquals(correctedDate, dao.getRiskSymptomsForOccurrence(downyId).single().observedAt)
            assertEquals(1, dao.removeOwnedOilSpot(
                1, 10, 20, downyId, 1000, symptomId, correctedDate, ""
            ))
            assertTrue(dao.getRiskSymptomsForOccurrence(downyId).isEmpty())
            assertNull(dao.observeOwnedBlockOccurrences(1, 10, 20).first().single { it.id == downyId }.oilSpotDate)
            assertEquals(symptomId, dao.findOilSpotSymptomId())
            assertNotNull(dao.getDiseaseOccurrenceById(downyId))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun completedActivityLinksAreScopedAndCanBeRemoved(): Unit = runBlocking {
        val name = "r09g-activity-link-${UUID.randomUUID()}.db"
        seed(name)
        val db = open(name)
        try {
            DiseaseCatalogueSeeder(db).seed()
            val disease = assertNotNull(db.diseaseDao().getDiseaseByCode("grape_botrytis"))
            val observations = db.diseaseOccurrenceDao()
            val activities = db.activityDao()
            val date = LocalDate(2026, 6, 15)
            val occurrenceId = assertNotNull(observations.insertOwnedBlockOccurrence(
                1, DiseaseOccurrenceEntity(
                    0, disease.diseaseId, 10, 20, date, 12, DiseaseOccurrenceStatus.ACTIVE,
                    null, 1000, 1000, null
                )
            ))
            val completed = ActivityEntity(
                activityId = 0, vineyardId = 10, blockId = 20,
                activityType = "Scouting", activityDescription = "Checked the rows",
                season = "2026", phenologicalStage = null, priority = ActivityPriority.MEDIUM,
                status = ActivityStatus.COMPLETED, plannedAt = date, completedAt = date,
                tool = null, notes = null, createdAt = 1000, updatedAt = 1000,
                treatmentProduct = null, treatmentProductQuantity = null, treatmentProductUnits = null,
                waterQuantity = null, treatmentProductRate = null
            )
            val completedId = activities.insertActivity(completed)
            val plannedId = activities.insertActivity(completed.copy(status = ActivityStatus.PLANNED, completedAt = null))
            val vineyardActivityId = activities.insertActivity(completed.copy(blockId = null))
            assertEquals(completedId, observations.observeCompletedActivitiesForLink(1, 10, 20).first().single().activityId)
            assertFalse(observations.linkOwnedCompletedActivity(2, 10, 20, occurrenceId, 1000, completedId, date, date))
            assertFalse(observations.linkOwnedCompletedActivity(1, 10, 20, occurrenceId, 1000, plannedId, date, date))
            assertFalse(observations.linkOwnedCompletedActivity(1, 10, 20, occurrenceId, 1000, vineyardActivityId, date, date))
            assertFalse(observations.linkOwnedCompletedActivity(1, 10, 20, occurrenceId, 999, completedId, date, date))
            assertTrue(observations.linkOwnedCompletedActivity(1, 10, 20, occurrenceId, 1000, completedId, date, date))
            assertFalse(observations.linkOwnedCompletedActivity(1, 10, 20, occurrenceId, 1000, completedId, date, date))
            assertEquals(2, activities.deleteActivitySafely(10, completedId))
            val link = observations.observeOwnedOccurrenceActivityLinks(1, 10, 20).first().single()
            assertEquals(occurrenceId, link.occurrenceId)
            assertEquals(date, link.appliedOn)
            assertEquals(0, observations.unlinkOwnedOccurrenceActivity(2, 10, 20, occurrenceId, completedId))
            assertEquals(1, observations.unlinkOwnedOccurrenceActivity(1, 10, 20, occurrenceId, completedId))
            assertTrue(observations.observeOwnedOccurrenceActivityLinks(1, 10, 20).first().isEmpty())
            assertNotNull(activities.getActivityById(10, completedId))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun locationABAAndNewerRequestsRejectOldCommitsWithoutDeletingHistory(): Unit=runBlocking {
        val name="r06b2-aba-${UUID.randomUUID()}.db"; seed(name); val db=open(name)
        try {
            val dao=db.weatherCacheDao(); val old=assertNotNull(dao.begin(10,42.1,24.7,"Europe/Sofia","forecast",1000))
            val rows=WeatherForecastMapper().weatherDtoToEntities(WeatherResponseFixtures.response(),10,1000,0)
            move(db,43.1); move(db,42.1)
            assertFalse(dao.completeForecast(old,rows,2000)); assertNotEquals(old.generation,dao.generation(10))
            assertNotNull(db.weatherHistoryDao().getLatestWeatherByVineyard(10))
            val next=assertNotNull(dao.begin(10,42.1,24.7,"Europe/Sofia","forecast",3000))
            val latest=assertNotNull(dao.begin(10,42.1,24.7,"Europe/Sofia","forecast",4000))
            assertFalse(dao.completeForecast(next,rows,5000)); assertTrue(dao.completeForecast(latest,rows,5000))
            assertFalse(dao.completeForecast(latest,rows,6000))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun legacySameHourIsRefetchedWithStableIdAndCurrentSourceRowsStayStable(): Unit=runBlocking {
        val name="r06b2-history-${UUID.randomUUID()}.db"; seed(name); val db=open(name)
        try {
            val dao=db.weatherCacheDao(); val old=assertNotNull(db.weatherHistoryDao().getLatestWeatherByVineyard(10))
            val dto=WeatherResponseFixtures.response(2,old.timestamp/1000)
            val rows=WeatherHistoryMapper().weatherDtoToEntity(dto,10,Long.MAX_VALUE)
            val request=assertNotNull(dao.begin(10,42.1,24.7,"Europe/Sofia","history",1000))
            assertEquals(1,dao.completeHistory(request,rows,2000))
            val stored=assertNotNull(dao.historyAt(10,old.timestamp)); assertEquals(old.weatherId,stored.weatherId); assertEquals(18.0,stored.hourlyTemp2m)
            assertEquals(2,dao.observeHistory(10,0).first().size)
            val archive=dao.archives(10).single(); assertNull(archive.generation); assertTrue(archive.rowJson.contains("0.0"))
            assertEquals(old.weatherId,archive.weatherId)
            val next=assertNotNull(dao.begin(10,42.1,24.7,"Europe/Sofia","history",3000))
            assertEquals(0,dao.completeHistory(next,rows.map { it.copy(hourlyTemp2m=19.0) },4000))
            assertEquals(18.0,dao.historyAt(10,old.timestamp)?.hourlyTemp2m)
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun failedAttemptSurvivesRestartAndRetainsPreviousCurrentSourceForecast(): Unit=runBlocking {
        val name="r06b2-status-${UUID.randomUUID()}.db"; seed(name); var db=open(name)
        try {
            var dao=db.weatherCacheDao(); val rows=WeatherForecastMapper().weatherDtoToEntities(WeatherResponseFixtures.response(),10,1000,0)
            val first=assertNotNull(dao.begin(10,42.1,24.7,"Europe/Sofia","forecast",1000)); assertTrue(dao.completeForecast(first,rows,2000))
            val failed=assertNotNull(dao.begin(10,42.1,24.7,"Europe/Sofia","forecast",3000)); dao.fail(failed,"timeout")
            db.close(); db=open(name); dao=db.weatherCacheDao()
            assertEquals("failed",dao.status(10,"forecast")?.state); assertEquals(2000L,dao.status(10,"forecast")?.succeededAt)
            assertEquals(48,dao.observeForecast(10).first().size)
            move(db,43.1); assertTrue(dao.observeForecast(10).first().isEmpty()); assertTrue(dao.observeStatus(10).first().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun migrationFailureRollsBackAndFreshCreationInstallsTheSameTriggers(): Unit=runBlocking {
        val name="r06b2-rollback-${UUID.randomUUID()}.db"; seed(name)
        val failing=object: androidx.room.migration.Migration(9,10) {
            override fun migrate(connection: SQLiteConnection) { MIGRATION_9_10.migrate(connection); error("Injected migration failure") }
        }
        val bad=Room.databaseBuilder<VineshieldDatabase>(context,name).setDriver(BundledSQLiteDriver()).addMigrations(failing, MIGRATION_10_11, MIGRATION_11_12).build()
        try { assertFailsWith<Exception> { bad.weatherCacheDao().generation(10) } } finally { bad.close() }
        raw(name) { c ->
            val q=c.prepare("SELECT count(*) FROM sqlite_master WHERE name='weather_source'")
            try { assertTrue(q.step()); assertEquals(0L,q.getLong(0)) } finally { q.close() }
        }
        val db=open(name)
        try { assertNotNull(db.weatherCacheDao().generation(10)) } finally { db.close(); context.deleteDatabase(name) }
        val fresh="r06b2-fresh-${UUID.randomUUID()}.db"; val empty=open(fresh)
        try { empty.weatherCacheDao().generation(10); empty.close()
            raw(fresh) { c -> val q=c.prepare("SELECT count(*) FROM sqlite_master WHERE type='trigger' AND name LIKE 'weather_source_%'")
                try { assertTrue(q.step()); assertEquals(2L,q.getLong(0)) } finally { q.close() }
            }
        } finally { empty.close(); context.deleteDatabase(fresh) }
    }
    @Test fun bookkeepingFailureRollsBackForecastReplacementAndCanBeRetried(): Unit=runBlocking {
        val name="r06b2-write-${UUID.randomUUID()}.db"; seed(name); var db=open(name)
        try {
            var dao=db.weatherCacheDao(); val rows=WeatherForecastMapper().weatherDtoToEntities(WeatherResponseFixtures.response(),10,1000,0)
            val first=assertNotNull(dao.begin(10,42.1,24.7,"Europe/Sofia","forecast",1000)); assertTrue(dao.completeForecast(first,rows,2000))
            val before=dao.observeForecast(10).first(); val pending=assertNotNull(dao.begin(10,42.1,24.7,"Europe/Sofia","forecast",3000))
            db.close(); raw(name) { it.execSQL("CREATE TRIGGER r06b2_fail_status BEFORE UPDATE ON weather_cache_status WHEN NEW.state='succeeded' BEGIN SELECT RAISE(ABORT,'Injected status failure'); END") }
            db=open(name); dao=db.weatherCacheDao()
            assertFailsWith<Exception> { dao.completeForecast(pending,rows.map { it.copy(hourlyTemp2m=19.0) },4000) }
            assertEquals(before,dao.observeForecast(10).first()); assertEquals("started",dao.status(10,"forecast")?.state)
            db.close(); raw(name) { it.execSQL("DROP TRIGGER r06b2_fail_status") }; db=open(name); dao=db.weatherCacheDao()
            assertTrue(dao.completeForecast(pending,rows,5000)); assertEquals("succeeded",dao.status(10,"forecast")?.state)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun archiveAndHistoryRepairRollBackTogetherOnProvenanceFailure(): Unit=runBlocking {
        val name="r06b2-archive-${UUID.randomUUID()}.db"; seed(name); var db=open(name)
        try {
            var dao=db.weatherCacheDao(); val old=assertNotNull(db.weatherHistoryDao().getLatestWeatherByVineyard(10))
            val rows=WeatherHistoryMapper().weatherDtoToEntity(WeatherResponseFixtures.response(1,old.timestamp/1000),10,Long.MAX_VALUE)
            val pending=assertNotNull(dao.begin(10,42.1,24.7,"Europe/Sofia","history",1000))
            db.close(); raw(name) { it.execSQL("CREATE TRIGGER r06b2_fail_provenance BEFORE INSERT ON weather_history_source BEGIN SELECT RAISE(ABORT,'Injected provenance failure'); END") }
            db=open(name); dao=db.weatherCacheDao()
            assertFailsWith<Exception> { dao.completeHistory(pending,rows,2000) }
            assertEquals(old,dao.historyAt(10,old.timestamp)); assertTrue(dao.archives(10).isEmpty())
            assertEquals("started",dao.status(10,"history")?.state)
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun deletionAndSameIdRecreationCannotAcceptPreviousRequest(): Unit=runBlocking {
        val name="r06b2-recreate-${UUID.randomUUID()}.db"; seed(name); val db=open(name)
        try {
            val dao=db.weatherCacheDao(); val old=assertNotNull(dao.vineyard(10))
            val pending=assertNotNull(dao.begin(10,42.1,24.7,"Europe/Sofia","forecast",1000))
            val rows=WeatherForecastMapper().weatherDtoToEntities(WeatherResponseFixtures.response(),10,1000,0)
            assertEquals(1,db.vineyardDao().deleteVineyard(1,10)); assertFalse(dao.completeForecast(pending,rows,2000))
            db.vineyardDao().insertVineyard(old)
            assertNotEquals(pending.generation,dao.generation(10)); assertFalse(dao.completeForecast(pending,rows,3000))
            assertTrue(dao.observeForecast(10).first().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }

}
