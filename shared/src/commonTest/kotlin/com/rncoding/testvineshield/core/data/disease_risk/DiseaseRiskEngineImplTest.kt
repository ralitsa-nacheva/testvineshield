
package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.domain.disease_risk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DiseaseRiskEngineImplTest {

    private val modelType =
        DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS

    private val diseaseId = 1L
    private val vineyardId = 2L
    private val blockId = 3L
    private val calculatedAt = 1_790_000_000_000L

    private val input = DiseaseRiskInput(
        diseaseId = diseaseId,
        vineyardId = vineyardId,
        blockId = blockId,
        timeZone = "Europe/Sofia",
        hourlyWeather = emptyList(),
        dailyWeather = emptyList(),
        hostContext = DiseaseRiskHostContext(
            phenologicalStage = null,
            activeOccurrence = null,
            observedSymptoms = emptyList()
        )
    )

    private val evaluation = DiseaseRiskModelEvaluation(
        result = DiseaseRiskResult(
            diseaseId = diseaseId,
            vineyardId = vineyardId,
            blockId = blockId,
            calculatedAt = calculatedAt,
            modelType = modelType,
            modelVersion = "1.0",
            evaluationStatus = DiseaseRiskEvaluationStatus.CALCULATED,
            riskLevel = DiseaseRiskLevel.LOW,
            confidence = DiseaseRiskConfidence.HIGH,
            event = null,
            nativeRiskValue = 10.0,
            factors = emptyList()
        ),
        nextState = null
    )

    private class TestState : DiseaseRiskModelState

    private inner class FakeModel : DiseaseRiskModel {
        override val modelType =
            this@DiseaseRiskEngineImplTest.modelType

        override val modelVersion = "1.0"

        var callCount = 0
        var receivedInput: DiseaseRiskInput? = null
        var receivedState: DiseaseRiskModelState? = null
        var receivedCalculatedAt: Long? = null

        override fun evaluate(
            input: DiseaseRiskInput,
            previousState: DiseaseRiskModelState?,
            calculatedAt: Long
        ): DiseaseRiskModelEvaluation {
            callCount++
            receivedInput = input
            receivedState = previousState
            receivedCalculatedAt = calculatedAt
            return evaluation
        }
    }

    private inner class FakeInputFactory : DiseaseRiskInputFactory {
        var result: DiseaseRiskInputResult =
            DiseaseRiskInputResult.Success(input)

        var callCount = 0
        var receivedState: DiseaseRiskModelState? = null

        override suspend fun createInput(
            modelType: DiseaseRiskModelType,
            diseaseId: Long,
            vineyardId: Long,
            blockId: Long,
            calculatedAt: Long,
            previousState: DiseaseRiskModelState?
        ): DiseaseRiskInputResult {
            callCount++
            receivedState = previousState
            return result
        }
    }

    private inner class FakePersistence : DiseaseRiskPersistence {
        var loadResult: DiseaseRiskPersistence.StateLoadResult =
            DiseaseRiskPersistence.StateLoadResult.NotFound

        var loadCount = 0
        var saveCount = 0

        var savedEvaluation: DiseaseRiskModelEvaluation? = null
        var saveException: Exception? = null
        var loadException: Exception? = null

        override suspend fun loadState(
            vineyardId: Long,
            blockId: Long,
            diseaseId: Long,
            model: DiseaseRiskModel
        ): DiseaseRiskPersistence.StateLoadResult {
            loadCount++
            loadException?.let { throw it }
            return loadResult
        }

        override suspend fun saveEvaluation(
            evaluation: DiseaseRiskModelEvaluation
        ) {
            saveCount++
            saveException?.let { throw it }
            savedEvaluation = evaluation
        }
    }

    private inner class Fixture(
        registerModel: Boolean = true
    ) {
        val model = FakeModel()
        val factory = FakeInputFactory()
        val persistence = FakePersistence()

        val engine = DiseaseRiskEngineImpl(
            modelRegistry = DiseaseRiskModelRegistry(
                if (registerModel) listOf(model) else emptyList()
            ),
            inputFactory = factory,
            persistence = persistence,
            coordinator = DiseaseRiskEvaluationCoordinator()
        )

        suspend fun evaluate(): DiseaseRiskEngineResult =
            engine.evaluate(
                modelType = modelType,
                diseaseId = diseaseId,
                vineyardId = vineyardId,
                blockId = blockId,
                calculatedAt = calculatedAt
            )
    }

    @Test
    fun successfulEvaluationIsPersisted() = runBlocking {
        val fixture = Fixture()

        val result = fixture.evaluate()

        val success =
            assertIs<DiseaseRiskEngineResult.Success>(result)

        assertSame(evaluation, success.evaluation)
        assertSame(evaluation, fixture.persistence.savedEvaluation)

        assertEquals(1, fixture.persistence.loadCount)
        assertEquals(1, fixture.factory.callCount)
        assertEquals(1, fixture.model.callCount)
        assertEquals(1, fixture.persistence.saveCount)

        assertSame(input, fixture.model.receivedInput)
        assertEquals(
            calculatedAt,
            fixture.model.receivedCalculatedAt
        )
    }

    @Test
    fun previousStateIsPassedToFactoryAndModel() = runBlocking {
        val fixture = Fixture()
        val state = TestState()

        fixture.persistence.loadResult =
            DiseaseRiskPersistence.StateLoadResult.Loaded(state)

        val result = fixture.evaluate()

        assertIs<DiseaseRiskEngineResult.Success>(result)
        assertSame(state, fixture.factory.receivedState)
        assertSame(state, fixture.model.receivedState)
    }

    @Test
    fun missingModelDoesNotLoadStateOrPersist() = runBlocking {
        val fixture = Fixture(registerModel = false)

        val result = fixture.evaluate()

        assertIs<DiseaseRiskEngineResult.Failure>(result)
        assertEquals(0, fixture.persistence.loadCount)
        assertEquals(0, fixture.factory.callCount)
        assertEquals(0, fixture.persistence.saveCount)
    }

    @Test
    fun incompatibleStateStopsEvaluation() = runBlocking {
        val fixture = Fixture()

        fixture.persistence.loadResult =
            DiseaseRiskPersistence.StateLoadResult.IncompatibleVersion(
                storedVersion = "0.9",
                expectedVersion = "1.0"
            )

        val result = fixture.evaluate()

        val incompatible =
            assertIs<DiseaseRiskEngineResult.StateIncompatible>(result)

        assertEquals("0.9", incompatible.storedVersion)
        assertEquals("1.0", incompatible.expectedVersion)

        assertEquals(0, fixture.factory.callCount)
        assertEquals(0, fixture.model.callCount)
        assertEquals(0, fixture.persistence.saveCount)
    }

    @Test
    fun invalidStateStopsEvaluation() = runBlocking {
        val fixture = Fixture()

        fixture.persistence.loadResult =
            DiseaseRiskPersistence.StateLoadResult.InvalidState(
                reason = "Corrupted model state"
            )

        val result = fixture.evaluate()

        val invalid =
            assertIs<DiseaseRiskEngineResult.StateInvalid>(result)

        assertEquals("Corrupted model state", invalid.reason)
        assertEquals(0, fixture.model.callCount)
        assertEquals(0, fixture.persistence.saveCount)
    }

    @Test
    fun insufficientWeatherDoesNotExecuteModel() = runBlocking {
        val fixture = Fixture()

        fixture.factory.result =
            DiseaseRiskInputResult.InsufficientWeather(
                reason = "Missing hourly observations"
            )

        val result = fixture.evaluate()

        val failure =
            assertIs<DiseaseRiskEngineResult.Failure>(result)

        assertEquals(
            "Missing hourly observations",
            failure.reason
        )
        assertEquals(0, fixture.model.callCount)
        assertEquals(0, fixture.persistence.saveCount)
    }


    @Test
    fun disabledMonitoringIsSkippedWithoutExecutingModel() = runBlocking {
        val fixture = Fixture()

        fixture.factory.result =
            DiseaseRiskInputResult.MonitoringDisabled(
                vineyardId = vineyardId,
                blockId = blockId,
                diseaseId = diseaseId,
                modelType = modelType
            )

        val result = fixture.evaluate()

        val skipped =
            assertIs<DiseaseRiskEngineResult.Skipped>(result)

        assertEquals(
            DiseaseRiskEngineResult.Skipped.SkipReason.MONITORING_DISABLED,
            skipped.reason
        )

        assertEquals(1, fixture.factory.callCount)
        assertEquals(0, fixture.model.callCount)
        assertEquals(0, fixture.persistence.saveCount)
    }



    @Test
    fun noNewWeatherIsSkippedWithoutExecutingModel() = runBlocking {
        val fixture = Fixture()

        fixture.factory.result =
            DiseaseRiskInputResult.NoNewWeather

        val result = fixture.evaluate()

        val skipped =
            assertIs<DiseaseRiskEngineResult.Skipped>(result)

        assertEquals(
            DiseaseRiskEngineResult.Skipped.SkipReason.NO_NEW_WEATHER,
            skipped.reason
        )

        assertEquals(1, fixture.factory.callCount)
        assertEquals(0, fixture.model.callCount)
        assertEquals(0, fixture.persistence.saveCount)
    }


    @Test
    fun persistenceFailureIsReported() = runBlocking {
        val fixture = Fixture()

        fixture.persistence.saveException =
            IllegalStateException("Database write failed")

        val result = fixture.evaluate()

        val failure =
            assertIs<DiseaseRiskEngineResult.Failure>(result)

        assertEquals("Database write failed", failure.reason)
        assertEquals(1, fixture.model.callCount)
        assertEquals(1, fixture.persistence.saveCount)
        assertNull(fixture.persistence.savedEvaluation)
    }

    @Test
    fun coroutineCancellationIsPropagated() = runBlocking {
        val fixture = Fixture()

        fixture.persistence.loadException =
            CancellationException("Evaluation cancelled")

        assertFailsWith<CancellationException> {
            fixture.evaluate()
        }

        assertEquals(0, fixture.model.callCount)
        assertEquals(0, fixture.persistence.saveCount)
    }
}
