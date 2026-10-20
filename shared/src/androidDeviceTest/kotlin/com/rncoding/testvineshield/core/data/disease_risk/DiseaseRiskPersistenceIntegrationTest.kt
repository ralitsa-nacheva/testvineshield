
package com.rncoding.testvineshield.core.data.disease_risk

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.domain.disease_risk.powdery.PowderyMildewRiskState
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEvaluationCoordinator
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelType
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskMonitoringConfiguration
import com.rncoding.testvineshield.core.domain.disease_risk.*
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertIs

@RunWith(AndroidJUnit4::class)
class DiseaseRiskPersistenceIntegrationTest {

    @Test
    fun savesEvaluationAndReadsItBackFromRoom() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val database =
            Room.inMemoryDatabaseBuilder<VineshieldDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()

        try {

            val testIds = database.insertDiseaseRiskTestData()

            val vineyardId = testIds.vineyardId
            val blockId = testIds.blockId
            val diseaseId = testIds.diseaseId


            // 2. Create a real persistence implementation.

            val persistence = DefaultDiseaseRiskPersistence(
                diseaseRiskDao = database.diseaseRiskDao(),
                stateCodec = DiseaseRiskStateCodec()
            )

            val modelType =
                DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS

            val calculatedAt = 1_792_000_000_000L

            // 3. Construct a deterministic evaluation.
            // This is test data, not a scientific model result.

            val evaluation = DiseaseRiskModelEvaluation(
                result = DiseaseRiskResult(
                    diseaseId = diseaseId,
                    vineyardId = vineyardId,
                    blockId = blockId,
                    calculatedAt = calculatedAt,
                    modelType = modelType,
                    modelVersion = "1.0",
                    evaluationStatus =
                        DiseaseRiskEvaluationStatus.CALCULATED,
                    riskLevel = DiseaseRiskLevel.MODERATE,
                    confidence = DiseaseRiskConfidence.HIGH,
                    event = null,
                    nativeRiskValue = 42.0,
                    factors = emptyList()
                ),
                nextState = null
            )

            // 4. Persist through the real persistence class.

            persistence.saveEvaluation(evaluation)

            // 5. Read through the real Room DAO.

            val stored = database.diseaseRiskDao()
                .getLatestEvaluation(
                    vineyardId = vineyardId,
                    blockId = blockId,
                    diseaseId = diseaseId,
                    modelType = modelType.name
                )

            assertNotNull(stored)

            assertEquals(diseaseId, stored.diseaseId)
            assertEquals(vineyardId, stored.vineyardId)
            assertEquals(blockId, stored.blockId)
            assertEquals(calculatedAt, stored.calculatedAt)
            assertEquals(modelType.name, stored.modelType)
            assertEquals("1.0", stored.modelVersion)

            assertEquals(
                DiseaseRiskEvaluationStatus.CALCULATED.name,
                stored.evaluationStatus
            )

            assertEquals(
                DiseaseRiskLevel.MODERATE.name,
                stored.riskLevel
            )

            assertEquals(
                DiseaseRiskConfidence.HIGH.name,
                stored.confidence
            )

            assertEquals(42.0, stored.nativeRiskValue)

            // 6. No nextState was supplied, so no state
            // record should have been created.

            val storedState = database.diseaseRiskDao()
                .getModelState(
                    vineyardId = vineyardId,
                    blockId = blockId,
                    diseaseId = diseaseId,
                    modelType = modelType.name
                )

            assertNull(storedState)

        } finally {
            database.close()
        }
    }


    @Test
    fun savesAndRestoresPowderyMildewModelState() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val database =
            Room.inMemoryDatabaseBuilder<VineshieldDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()

        try {
            // Use the same user, vineyard, block and disease
            // insertion setup as the previous passing test.
            //
            // The IDs below must be the IDs returned/inserted
            // by that setup.


            val testIds = database.insertDiseaseRiskTestData()

            val vineyardId = testIds.vineyardId
            val blockId = testIds.blockId
            val diseaseId = testIds.diseaseId


            val persistence = DefaultDiseaseRiskPersistence(
                diseaseRiskDao = database.diseaseRiskDao(),
                stateCodec = DiseaseRiskStateCodec()
            )

            val model =
                com.rncoding.testvineshield.core.domain.disease_risk.powdery
                    .PowderyMildewUcDavisDiseaseRiskModel()

            val originalState = PowderyMildewRiskState(
                initiated = true,
                currentIndex = 60,
                consecutiveQualifyingDays = 0,
                lastEvaluatedDate = LocalDate(2026, 10, 19)
            )

            val evaluation = DiseaseRiskModelEvaluation(
                result = DiseaseRiskResult(
                    diseaseId = diseaseId,
                    vineyardId = vineyardId,
                    blockId = blockId,
                    calculatedAt = 1_792_000_000_000L,
                    modelType = model.modelType,
                    modelVersion = model.modelVersion,
                    evaluationStatus =
                        DiseaseRiskEvaluationStatus.CALCULATED,
                    riskLevel = DiseaseRiskLevel.MODERATE,
                    confidence = DiseaseRiskConfidence.HIGH,
                    event = null,
                    nativeRiskValue = 60.0,
                    factors = emptyList()
                ),
                nextState = originalState
            )

            persistence.saveEvaluation(evaluation)

            // Verify the risk evaluation was saved.
            val storedEvaluation = database.diseaseRiskDao()
                .getLatestEvaluation(
                    vineyardId = vineyardId,
                    blockId = blockId,
                    diseaseId = diseaseId,
                    modelType = model.modelType.name
                )

            assertNotNull(storedEvaluation)
            assertEquals(60.0, storedEvaluation.nativeRiskValue)

            // Verify the serialized state was saved.
            val storedState = database.diseaseRiskDao()
                .getModelState(
                    vineyardId = vineyardId,
                    blockId = blockId,
                    diseaseId = diseaseId,
                    modelType = model.modelType.name
                )

            assertNotNull(storedState)
            assertEquals(model.modelVersion, storedState.modelVersion)

            assertEquals(
                DiseaseRiskStateCodec.POWDERY_STATE_TYPE,
                storedState.stateType
            )

            // Load through persistence, which also decodes the state.
            val loaded = persistence.loadState(
                vineyardId = vineyardId,
                blockId = blockId,
                diseaseId = diseaseId,
                model = model
            )

            val restored =
                assertIs<DiseaseRiskPersistence.StateLoadResult.Loaded>(
                    loaded
                )

            assertEquals(originalState, restored.state)

        } finally {
            database.close()
        }
    }


    @Test
    fun detectsIncompatiblePersistedModelVersion() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val database =
            Room.inMemoryDatabaseBuilder<VineshieldDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()

        try {
            // Insert the required parent records, using the same
            // setup as your existing passing integration test.


            val testIds = database.insertDiseaseRiskTestData()

            val vineyardId = testIds.vineyardId
            val blockId = testIds.blockId
            val diseaseId = testIds.diseaseId

            val model =
                com.rncoding.testvineshield.core.domain.disease_risk.powdery
                    .PowderyMildewUcDavisDiseaseRiskModel()

            val persistence = DefaultDiseaseRiskPersistence(
                diseaseRiskDao = database.diseaseRiskDao(),
                stateCodec = DiseaseRiskStateCodec()
            )

            database.diseaseRiskDao().upsertModelState(
                com.rncoding.testvineshield.core.data.local.database.entities
                    .DiseaseRiskModelStateEntity(
                        vineyardId = vineyardId,
                        blockId = blockId,
                        diseaseId = diseaseId,
                        modelType = model.modelType.name,
                        modelVersion = "outdated-test-version",
                        stateType =
                            DiseaseRiskStateCodec.POWDERY_STATE_TYPE,
                        statePayload = "{}",
                        updatedAt = 1_792_000_000_000L
                    )
            )

            val result = persistence.loadState(
                vineyardId = vineyardId,
                blockId = blockId,
                diseaseId = diseaseId,
                model = model
            )

            val incompatible =
                assertIs<
                        DiseaseRiskPersistence.StateLoadResult.IncompatibleVersion
                        >(result)

            assertEquals(
                "outdated-test-version",
                incompatible.storedVersion
            )
            assertEquals(
                model.modelVersion,
                incompatible.expectedVersion
            )

        } finally {
            database.close()
        }
    }


    @Test
    fun monitoringConfigurationIsSavedAndRestoredFromRoom() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val database =
            Room.inMemoryDatabaseBuilder<VineshieldDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()

        try {


            val testIds = database.insertDiseaseRiskTestData()

            val vineyardId = testIds.vineyardId
            val blockId = testIds.blockId
            val diseaseId = testIds.diseaseId


            val repository =
                DiseaseRiskMonitoringConfigurationRepositoryImpl(
                    dao = database.diseaseRiskMonitoringConfigurationDao(),
                    coordinator = DiseaseRiskEvaluationCoordinator()
                )

            val modelType =
                DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS

            val configuration = DiseaseRiskMonitoringConfiguration(
                vineyardId = vineyardId,
                blockId = blockId,
                diseaseId = diseaseId,
                modelType = modelType,
                monitoringStartDate = LocalDate(2026, 10, 1),
                enabled = true
            )

            // 1. Save through the actual repository.
            repository.saveConfiguration(configuration)

            // 2. Read through the actual repository.
            val restored = repository.getConfiguration(
                vineyardId = vineyardId,
                blockId = blockId,
                diseaseId = diseaseId,
                modelType = modelType
            )

            assertEquals(configuration, restored)

            // 3. Disable monitoring without changing its start date.
            repository.saveConfiguration(
                configuration.copy(enabled = false)
            )

            val disabled = repository.getConfiguration(
                vineyardId = vineyardId,
                blockId = blockId,
                diseaseId = diseaseId,
                modelType = modelType
            )

            assertNotNull(disabled)
            assertEquals(false, disabled.enabled)

            // 4. Delete configuration.
            repository.deleteConfiguration(
                vineyardId = vineyardId,
                blockId = blockId,
                diseaseId = diseaseId,
                modelType = modelType
            )

            val deleted = repository.getConfiguration(
                vineyardId = vineyardId,
                blockId = blockId,
                diseaseId = diseaseId,
                modelType = modelType
            )

            assertNull(deleted)

        } finally {
            database.close()
        }
    }


    @Test
    fun listsOnlyEnabledMonitoringConfigurationsForVineyard() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val database =
            Room.inMemoryDatabaseBuilder<VineshieldDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()

        try {
            val ids = database.insertDiseaseRiskTestData()

            val repository =
                DiseaseRiskMonitoringConfigurationRepositoryImpl(
                    dao = database.diseaseRiskMonitoringConfigurationDao(),
                    coordinator = DiseaseRiskEvaluationCoordinator()
                )

            val enabledConfiguration =
                DiseaseRiskMonitoringConfiguration(
                    vineyardId = ids.vineyardId,
                    blockId = ids.blockId,
                    diseaseId = ids.diseaseId,
                    modelType =
                        DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS,
                    monitoringStartDate = LocalDate(2026, 10, 1),
                    enabled = true
                )

            val disabledConfiguration =
                enabledConfiguration.copy(
                    modelType =
                        DiseaseRiskModelType.POWDERY_MILDEW_ASCOSPORE,
                    enabled = false
                )

            val anotherEnabledConfiguration =
                enabledConfiguration.copy(
                    modelType = DiseaseRiskModelType.BOTRYTIS
                )

            repository.saveConfiguration(enabledConfiguration)
            repository.saveConfiguration(disabledConfiguration)
            repository.saveConfiguration(anotherEnabledConfiguration)

            // Only the two enabled configurations should be returned.
            val enabled = repository.getEnabledConfigurationsForVineyard(
                ids.vineyardId
            )

            assertEquals(2, enabled.size)
            assertEquals(
                setOf(
                    DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS,
                    DiseaseRiskModelType.BOTRYTIS
                ),
                enabled.map { it.modelType }.toSet()
            )

            assertTrue(enabled.all { it.enabled })
            assertTrue(enabled.all { it.vineyardId == ids.vineyardId })

            // A vineyard with no configurations returns an empty list.
            val otherVineyardConfigurations =
                repository.getEnabledConfigurationsForVineyard(
                    ids.vineyardId + 999L
                )

            assertTrue(otherVineyardConfigurations.isEmpty())

            // Disable one of the previously enabled configurations.
            repository.saveConfiguration(
                enabledConfiguration.copy(enabled = false)
            )

            val afterDisabling =
                repository.getEnabledConfigurationsForVineyard(
                    ids.vineyardId
                )

            assertEquals(1, afterDisabling.size)
            assertEquals(
                DiseaseRiskModelType.BOTRYTIS,
                afterDisabling.single().modelType
            )

        } finally {
            database.close()
        }
    }


}
