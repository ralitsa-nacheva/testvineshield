package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEngine
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelRegistry
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskPersistence
import com.rncoding.testvineshield.core.domain.disease_risk.VineyardDiseaseRiskEvaluator
import com.rncoding.testvineshield.di.commonModule
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class DiseaseRiskEngineIntegrationTest {

    @Test
    fun engineResolvesThroughKoinWithRealRoomDependencies() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val database = Room.inMemoryDatabaseBuilder<VineshieldDatabase>(
            context
        )
            .setDriver(BundledSQLiteDriver())
            .build()

        val testDatabaseModule = module {
            single<VineshieldDatabase> { database }
        }

        val app = koinApplication {
            modules(
                commonModule,
                testDatabaseModule
            )
        }

        try {
            val koin = app.koin

            // Resolve the real engine through Koin.
            val engine = koin.get<DiseaseRiskEngine>()
            assertNotNull(engine)

            // Verify singleton resolution.
            assertSame(
                engine,
                koin.get<DiseaseRiskEngine>()
            )

            // Resolve the real model registry.
            val registry = koin.get<DiseaseRiskModelRegistry>()

            assertEquals(
                5,
                registry.getRegisteredTypes().size
            )

            // Resolve the real Room-backed persistence.
            val persistence = koin.get<DiseaseRiskPersistence>()

            val model = registry.getAllModels().first()

            // A fresh database should contain no previous model state.
            val state = persistence.loadState(
                vineyardId = 1L,
                blockId = 1L,
                diseaseId = 1L,
                model = model
            )

            assertIs<DiseaseRiskPersistence.StateLoadResult.NotFound>(
                state
            )

            // Verify the actual Room DAO is usable.
            val storedState = database.diseaseRiskDao().getModelState(
                vineyardId = 1L,
                blockId = 1L,
                diseaseId = 1L,
                modelType = model.modelType.name
            )

            assertTrue(storedState == null)

        } finally {
            app.close()
            database.close()
        }
    }


    @Test
    fun vineyardDiseaseRiskEvaluatorResolvesThroughKoin(): Unit = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val database =
            Room.inMemoryDatabaseBuilder<VineshieldDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()

        val testDatabaseModule = module {
            single<VineshieldDatabase> { database }
        }

        val app = koinApplication {
            modules(commonModule, testDatabaseModule)
        }

        try {
            val evaluator =
                app.koin.get<VineyardDiseaseRiskEvaluator>()

            assertSame(
                evaluator,
                app.koin.get<VineyardDiseaseRiskEvaluator>()
            )

            // No monitoring configurations exist in this database.
            // The evaluator should return an empty summary.
            val summary = evaluator.evaluateVineyard(
                vineyardId = 1L,
                calculatedAt = 1_792_000_000_000L
            )

            assertEquals(1L, summary.vineyardId)
            assertEquals(0, summary.total)
            assertEquals(0, summary.succeeded)
            assertEquals(0, summary.skipped)
            assertEquals(0, summary.failed)
            assertTrue(summary.items.isEmpty())

        } finally {
            app.close()
            database.close()
        }
    }

}