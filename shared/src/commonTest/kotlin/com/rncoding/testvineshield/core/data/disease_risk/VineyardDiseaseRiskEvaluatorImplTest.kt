
package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.domain.disease_risk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VineyardDiseaseRiskEvaluatorImplTest {

    private val vineyardId = 10L
    private val calculatedAt = 1_792_000_000_000L

    private fun configuration(
        blockId: Long,
        enabled: Boolean = true
    ) = DiseaseRiskMonitoringConfiguration(
        vineyardId = vineyardId,
        blockId = blockId,
        diseaseId = 20L,
        modelType = DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS,
        monitoringStartDate = LocalDate(2026, 10, 1),
        enabled = enabled
    )

    private class FakeMonitoringRepository :
        DiseaseRiskMonitoringConfigurationRepository {

        var configurations =
            emptyList<DiseaseRiskMonitoringConfiguration>()

        var requestedVineyardId: Long? = null
        var loadException: Exception? = null

        override suspend fun getEnabledConfigurationsForVineyard(
            vineyardId: Long
        ): List<DiseaseRiskMonitoringConfiguration> {
            requestedVineyardId = vineyardId
            loadException?.let { throw it }
            return configurations.filter {
                it.vineyardId == vineyardId && it.enabled
            }
        }

        override suspend fun getConfiguration(
            vineyardId: Long,
            blockId: Long,
            diseaseId: Long,
            modelType: DiseaseRiskModelType
        ): DiseaseRiskMonitoringConfiguration? =
            configurations.firstOrNull {
                it.vineyardId == vineyardId &&
                        it.blockId == blockId &&
                        it.diseaseId == diseaseId &&
                        it.modelType == modelType
            }

        override suspend fun saveConfiguration(
            configuration: DiseaseRiskMonitoringConfiguration
        ) {
            configurations = configurations.filterNot {
                it.vineyardId == configuration.vineyardId &&
                        it.blockId == configuration.blockId &&
                        it.diseaseId == configuration.diseaseId &&
                        it.modelType == configuration.modelType
            } + configuration
        }

        override suspend fun deleteConfiguration(
            vineyardId: Long,
            blockId: Long,
            diseaseId: Long,
            modelType: DiseaseRiskModelType
        ) {
            configurations = configurations.filterNot {
                it.vineyardId == vineyardId &&
                        it.blockId == blockId &&
                        it.diseaseId == diseaseId &&
                        it.modelType == modelType
            }
        }
    }

    private class FakeEngine : DiseaseRiskEngine {

        data class Call(
            val modelType: DiseaseRiskModelType,
            val diseaseId: Long,
            val vineyardId: Long,
            val blockId: Long,
            val calculatedAt: Long
        )

        val calls = mutableListOf<Call>()

        var resultForBlock: (Long) -> DiseaseRiskEngineResult = {
            DiseaseRiskEngineResult.Skipped(
                DiseaseRiskEngineResult.Skipped
                    .SkipReason.NO_NEW_WEATHER
            )
        }

        override suspend fun evaluate(
            modelType: DiseaseRiskModelType,
            diseaseId: Long,
            vineyardId: Long,
            blockId: Long,
            calculatedAt: Long
        ): DiseaseRiskEngineResult {
            calls += Call(
                modelType,
                diseaseId,
                vineyardId,
                blockId,
                calculatedAt
            )

            return resultForBlock(blockId)
        }
    }

    @Test
    fun emptyConfigurationListReturnsEmptySummary() {
        runBlocking {
            val repository = FakeMonitoringRepository()
            val engine = FakeEngine()

            val evaluator = VineyardDiseaseRiskEvaluatorImpl(
                monitoringRepository = repository,
                diseaseRiskEngine = engine
            )

            val summary = evaluator.evaluateVineyard(
                vineyardId,
                calculatedAt
            )

            assertEquals(vineyardId, repository.requestedVineyardId)
            assertEquals(vineyardId, summary.vineyardId)
            assertEquals(calculatedAt, summary.calculatedAt)
            assertEquals(0, summary.total)
            assertEquals(0, summary.succeeded)
            assertEquals(0, summary.skipped)
            assertEquals(0, summary.failed)
            assertTrue(summary.items.isEmpty())
            assertTrue(engine.calls.isEmpty())
        }
    }

    @Test
    fun evaluatesOnlyEnabledConfigurationsForRequestedVineyard() {
        runBlocking {
            val repository = FakeMonitoringRepository()
            val engine = FakeEngine()

            repository.configurations = listOf(
                configuration(blockId = 1L),
                configuration(blockId = 2L, enabled = false),
                configuration(blockId = 3L),
                configuration(blockId = 4L).copy(
                    vineyardId = 999L
                )
            )

            val evaluator = VineyardDiseaseRiskEvaluatorImpl(
                repository,
                engine
            )

            val summary = evaluator.evaluateVineyard(
                vineyardId,
                calculatedAt
            )

            assertEquals(2, summary.total)
            assertEquals(2, summary.skipped)
            assertEquals(0, summary.failed)

            assertEquals(
                listOf(1L, 3L),
                engine.calls.map { it.blockId }
            )

            assertTrue(
                engine.calls.all {
                    it.vineyardId == vineyardId &&
                            it.calculatedAt == calculatedAt
                }
            )
        }
    }

    @Test
    fun failureInOneConfigurationDoesNotStopNextEvaluation() {
        runBlocking {
            val repository = FakeMonitoringRepository()
            val engine = FakeEngine()

            repository.configurations = listOf(
                configuration(1L),
                configuration(2L),
                configuration(3L)
            )

            engine.resultForBlock = { blockId ->
                when (blockId) {
                    1L -> DiseaseRiskEngineResult.Failure(
                        "Model evaluation failed"
                    )

                    2L -> DiseaseRiskEngineResult.Skipped(
                        DiseaseRiskEngineResult.Skipped
                            .SkipReason.NO_NEW_WEATHER
                    )

                    else -> DiseaseRiskEngineResult.StateInvalid(
                        "Corrupted state"
                    )
                }
            }

            val evaluator = VineyardDiseaseRiskEvaluatorImpl(
                repository,
                engine
            )

            val summary = evaluator.evaluateVineyard(
                vineyardId,
                calculatedAt
            )

            assertEquals(3, summary.total)
            assertEquals(0, summary.succeeded)
            assertEquals(1, summary.skipped)
            assertEquals(2, summary.failed)
            assertTrue(summary.hasFailures)

            assertEquals(
                listOf(1L, 2L, 3L),
                engine.calls.map { it.blockId }
            )

            assertIs<DiseaseRiskEngineResult.Failure>(
                summary.items[0].result
            )
            assertIs<DiseaseRiskEngineResult.Skipped>(
                summary.items[1].result
            )
            assertIs<DiseaseRiskEngineResult.StateInvalid>(
                summary.items[2].result
            )
        }
    }

    @Test
    fun unexpectedEngineExceptionBecomesFailureAndProcessingContinues() {
        runBlocking {
            val repository = FakeMonitoringRepository()
            val engine = FakeEngine()

            repository.configurations = listOf(
                configuration(1L),
                configuration(2L)
            )

            engine.resultForBlock = { blockId ->
                if (blockId == 1L) {
                    throw IllegalStateException("Unexpected model error")
                }

                DiseaseRiskEngineResult.Skipped(
                    DiseaseRiskEngineResult.Skipped
                        .SkipReason.NO_NEW_WEATHER
                )
            }

            val summary = VineyardDiseaseRiskEvaluatorImpl(
                repository,
                engine
            ).evaluateVineyard(vineyardId, calculatedAt)

            assertEquals(2, summary.total)
            assertEquals(1, summary.failed)
            assertEquals(1, summary.skipped)

            val failure = assertIs<DiseaseRiskEngineResult.Failure>(
                summary.items[0].result
            )

            assertEquals("Unexpected model error", failure.reason)
            assertEquals(2, engine.calls.size)
        }
    }

    @Test
    fun configurationLoadingFailurePropagates() {
        runBlocking {
            val repository = FakeMonitoringRepository()
            val engine = FakeEngine()

            repository.loadException =
                IllegalStateException("Database unavailable")

            val evaluator = VineyardDiseaseRiskEvaluatorImpl(
                repository,
                engine
            )

            val exception = assertFailsWith<IllegalStateException> {
                evaluator.evaluateVineyard(vineyardId, calculatedAt)
            }

            assertEquals("Database unavailable", exception.message)
            assertTrue(engine.calls.isEmpty())
        }
    }

    @Test
    fun cancellationStopsRemainingEvaluations() {
        runBlocking {
            val repository = FakeMonitoringRepository()
            val engine = FakeEngine()

            repository.configurations = listOf(
                configuration(1L),
                configuration(2L),
                configuration(3L)
            )

            engine.resultForBlock = { blockId ->
                if (blockId == 2L) {
                    throw CancellationException("Evaluation cancelled")
                }

                DiseaseRiskEngineResult.Skipped(
                    DiseaseRiskEngineResult.Skipped
                        .SkipReason.NO_NEW_WEATHER
                )
            }

            val evaluator = VineyardDiseaseRiskEvaluatorImpl(
                repository,
                engine
            )

            assertFailsWith<CancellationException> {
                evaluator.evaluateVineyard(vineyardId, calculatedAt)
            }

            assertEquals(
                listOf(1L, 2L),
                engine.calls.map { it.blockId }
            )
        }
    }

    @Test
    fun invalidArgumentsAreRejectedBeforeRepositoryAccess() {
        runBlocking {
            val repository = FakeMonitoringRepository()
            val evaluator = VineyardDiseaseRiskEvaluatorImpl(
                repository,
                FakeEngine()
            )

            assertFailsWith<IllegalArgumentException> {
                evaluator.evaluateVineyard(0L, calculatedAt)
            }

            assertFailsWith<IllegalArgumentException> {
                evaluator.evaluateVineyard(vineyardId, 0L)
            }

            assertEquals(null, repository.requestedVineyardId)
        }
    }


}
