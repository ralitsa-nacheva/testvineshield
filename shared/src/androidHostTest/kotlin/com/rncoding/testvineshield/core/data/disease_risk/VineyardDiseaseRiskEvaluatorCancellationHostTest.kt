
package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEngine
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEngineResult
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelEvaluation
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskMonitoringConfiguration
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskMonitoringConfigurationRepository
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelType

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.coroutines.CompletableDeferred

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VineyardDiseaseRiskEvaluatorCancellationHostTest {

    private val monitoringRepository =
        mockk<DiseaseRiskMonitoringConfigurationRepository>()

    private val diseaseRiskEngine =
        mockk<DiseaseRiskEngine>()

    private val evaluator = VineyardDiseaseRiskEvaluatorImpl(
        monitoringRepository = monitoringRepository,
        diseaseRiskEngine = diseaseRiskEngine
    )

    private val vineyardId = 10L
    private val calculatedAt = 1_700_000_000_000L

    private fun configuration(
        blockId: Long,
        diseaseId: Long
    ): DiseaseRiskMonitoringConfiguration {

        val config = mockk<DiseaseRiskMonitoringConfiguration>()

        val modelType = DiseaseRiskModelType.entries.first()

        every { config.vineyardId } returns vineyardId
        every { config.blockId } returns blockId
        every { config.diseaseId } returns diseaseId
        every { config.modelType } returns modelType

        return config
    }

    private fun stubEngine(
        configuration: DiseaseRiskMonitoringConfiguration,
        result: DiseaseRiskEngineResult
    ) {
        // Read values before entering the MockK recording block.
        val modelType = configuration.modelType
        val diseaseId = configuration.diseaseId
        val vineyardId = configuration.vineyardId
        val blockId = configuration.blockId

        coEvery {
            diseaseRiskEngine.evaluate(
                modelType = modelType,
                diseaseId = diseaseId,
                vineyardId = vineyardId,
                blockId = blockId,
                calculatedAt = calculatedAt
            )
        } returns result
    }

    @Test
    fun noConfigurationsReturnsEmptySummary(): Unit = runBlocking {
        coEvery {
            monitoringRepository.getEnabledConfigurationsForVineyard(vineyardId)
        } returns emptyList()

        val summary = evaluator.evaluateVineyard(
            vineyardId = vineyardId,
            calculatedAt = calculatedAt
        )

        assertEquals(vineyardId, summary.vineyardId)
        assertEquals(calculatedAt, summary.calculatedAt)
        assertTrue(summary.items.isEmpty())
        assertEquals(0, summary.total)
        assertEquals(0, summary.succeeded)
        assertEquals(0, summary.failed)
        assertFalse(summary.hasFailures)

        coVerify(exactly = 0) {
            diseaseRiskEngine.evaluate(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun successfulEvaluationAppearsInSummary(): Unit = runBlocking {
        val config = configuration(blockId = 1L, diseaseId = 100L)

        coEvery {
            monitoringRepository.getEnabledConfigurationsForVineyard(vineyardId)
        } returns listOf(config)

        val success = DiseaseRiskEngineResult.Success(
            evaluation = mockk<DiseaseRiskModelEvaluation>()
        )

        stubEngine(config, success)

        val summary = evaluator.evaluateVineyard(vineyardId, calculatedAt)

        assertEquals(1, summary.total)

        assertEquals(
            expected = success,
            actual = summary.items.single().result,
            message = "The engine result was not preserved."
        )

        assertEquals(1, summary.succeeded)
        assertEquals(0, summary.failed)
        assertFalse(summary.hasFailures)
        assertEquals(config, summary.items.single().configuration)
        assertEquals(success, summary.items.single().result)
    }

    @Test
    fun multipleConfigurationsAreEvaluated(): Unit = runBlocking {
        val first = configuration(blockId = 1L, diseaseId = 100L)
        val second = configuration(blockId = 2L, diseaseId = 200L)

        coEvery {
            monitoringRepository.getEnabledConfigurationsForVineyard(vineyardId)
        } returns listOf(first, second)

        stubEngine(
            first,
            DiseaseRiskEngineResult.Skipped(
                DiseaseRiskEngineResult.Skipped.SkipReason.NO_NEW_WEATHER
            )
        )

        stubEngine(
            second,
            DiseaseRiskEngineResult.Failure("Missing weather data")
        )

        val summary = evaluator.evaluateVineyard(vineyardId, calculatedAt)

        assertEquals(2, summary.total)
        assertEquals(1, summary.skipped)
        assertEquals(1, summary.failed)
        assertTrue(summary.hasFailures)

        val modelType = DiseaseRiskModelType.entries.first()

        coVerify(exactly = 1) {
            diseaseRiskEngine.evaluate(
                modelType = modelType,
                diseaseId = 100L,
                vineyardId = 10L,
                blockId = 1L,
                calculatedAt = calculatedAt
            )
        }

        coVerify(exactly = 1) {
            diseaseRiskEngine.evaluate(
                modelType = modelType,
                diseaseId = 200L,
                vineyardId = 10L,
                blockId = 2L,
                calculatedAt = calculatedAt
            )
        }
    }

    @Test
    fun engineFailureIsPreservedInSummary(): Unit = runBlocking {
        val config = configuration(1L, 100L)

        coEvery {
            monitoringRepository.getEnabledConfigurationsForVineyard(vineyardId)
        } returns listOf(config)

        val failure = DiseaseRiskEngineResult.Failure(
            reason = "Model calculation failed"
        )

        stubEngine(config, failure)

        val summary = evaluator.evaluateVineyard(vineyardId, calculatedAt)

        assertEquals(failure, summary.items.single().result)
        assertEquals(1, summary.failed)
        assertTrue(summary.hasFailures)
    }

    @Test
    fun ordinaryEngineExceptionBecomesFailure(): Unit = runBlocking {
        val config = configuration(1L, 100L)

        coEvery {
            monitoringRepository.getEnabledConfigurationsForVineyard(vineyardId)
        } returns listOf(config)

        val modelType = config.modelType
        val diseaseId = config.diseaseId
        val configVineyardId = config.vineyardId
        val blockId = config.blockId

        coEvery {
            diseaseRiskEngine.evaluate(
                modelType = modelType,
                diseaseId = diseaseId,
                vineyardId = configVineyardId,
                blockId = blockId,
                calculatedAt = calculatedAt
            )
        } throws IllegalStateException("Unexpected engine error")

        val summary = evaluator.evaluateVineyard(vineyardId, calculatedAt)

        val failure = assertIs<DiseaseRiskEngineResult.Failure>(
            summary.items.single().result
        )

        assertEquals("Unexpected engine error", failure.reason)
        assertEquals(1, summary.failed)
    }

    @Test
    fun engineCancellationIsPropagated(): Unit = runBlocking {
        val config = configuration(1L, 100L)

        coEvery {
            monitoringRepository.getEnabledConfigurationsForVineyard(vineyardId)
        } returns listOf(config)

        val modelType = config.modelType
        val diseaseId = config.diseaseId
        val configVineyardId = config.vineyardId
        val blockId = config.blockId

        coEvery {
            diseaseRiskEngine.evaluate(
                modelType = modelType,
                diseaseId = diseaseId,
                vineyardId = configVineyardId,
                blockId = blockId,
                calculatedAt = calculatedAt
            )
        } throws CancellationException("Worker cancelled")

        assertFailsWith<CancellationException> {
            evaluator.evaluateVineyard(vineyardId, calculatedAt)
        }
    }


    @Test
    fun cancellationBetweenConfigurationsStopsFurtherEvaluation(): Unit = runBlocking {
        val first = configuration(1L, 100L)
        val second = configuration(2L, 200L)

        coEvery {
            monitoringRepository.getEnabledConfigurationsForVineyard(vineyardId)
        } returns listOf(first, second)

        val modelType = DiseaseRiskModelType.entries.first()

        // Signals that the first evaluation has actually started.
        val firstEvaluationStarted = CompletableDeferred<Unit>()

        // Keeps the first evaluation suspended until cancellation.
        val releaseFirstEvaluation = CompletableDeferred<Unit>()

        coEvery {
            diseaseRiskEngine.evaluate(
                modelType = modelType,
                diseaseId = 100L,
                vineyardId = 10L,
                blockId = 1L,
                calculatedAt = calculatedAt
            )
        } coAnswers {
            firstEvaluationStarted.complete(Unit)

            // This suspension is cancellable.
            releaseFirstEvaluation.await()

            DiseaseRiskEngineResult.Skipped(
                DiseaseRiskEngineResult.Skipped.SkipReason.NO_NEW_WEATHER
            )
        }

        val job = async {
            evaluator.evaluateVineyard(
                vineyardId = vineyardId,
                calculatedAt = calculatedAt
            )
        }

        // Wait until the first engine evaluation has started.
        firstEvaluationStarted.await()

        // Cancel while the first evaluation is suspended.
        job.cancel()

        assertFailsWith<CancellationException> {
            job.await()
        }

        // The first configuration was evaluated.
        coVerify(exactly = 1) {
            diseaseRiskEngine.evaluate(
                modelType = modelType,
                diseaseId = 100L,
                vineyardId = 10L,
                blockId = 1L,
                calculatedAt = calculatedAt
            )
        }

        // Cancellation prevented the second configuration.
        coVerify(exactly = 0) {
            diseaseRiskEngine.evaluate(
                modelType = modelType,
                diseaseId = 200L,
                vineyardId = 10L,
                blockId = 2L,
                calculatedAt = calculatedAt
            )
        }
    }

    @Test
    fun configurationRepositoryExceptionIsPropagated(): Unit = runBlocking {
        coEvery {
            monitoringRepository.getEnabledConfigurationsForVineyard(vineyardId)
        } throws IllegalStateException("Configuration database unavailable")

        val exception = assertFailsWith<IllegalStateException> {
            evaluator.evaluateVineyard(vineyardId, calculatedAt)
        }

        assertEquals(
            "Configuration database unavailable",
            exception.message
        )
    }

    @Test
    fun invalidVineyardIdIsRejected(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            evaluator.evaluateVineyard(
                vineyardId = 0L,
                calculatedAt = calculatedAt
            )
        }
    }

    @Test
    fun invalidCalculationTimestampIsRejected(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            evaluator.evaluateVineyard(
                vineyardId = vineyardId,
                calculatedAt = 0L
            )
        }
    }
}
