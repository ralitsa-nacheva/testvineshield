package com.rncoding.testvineshield.core.domain.disease_risk.botrytis

import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEvaluationStatus
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskHostContext
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskInput
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskLevel
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint
import com.rncoding.testvineshield.core.domain.disease_risk.EnvironmentalMeasurement
import com.rncoding.testvineshield.core.domain.disease_risk.MeasurementConfidence
import com.rncoding.testvineshield.core.domain.disease_risk.MeasurementSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BotrytisStageAvailabilityTest {
    private val model = BotrytisDiseaseRiskModel()

    @Test
    fun unknownStageWithWeatherRequiresMoreData() {
        val evaluation = evaluate(stage = null, weather = dryWeather())
        assertEquals(DiseaseRiskEvaluationStatus.INSUFFICIENT_DATA, evaluation.result.evaluationStatus)
        assertNull(evaluation.result.nativeRiskValue)
        assertNull(evaluation.nextState)
        assertTrue(evaluation.result.factors.isEmpty())
    }

    @Test
    fun unknownStageWithoutWeatherRequiresMoreData() {
        assertEquals(
            DiseaseRiskEvaluationStatus.INSUFFICIENT_DATA,
            evaluate(stage = null, weather = emptyList()).result.evaluationStatus
        )
    }

    @Test
    fun knownStageOutsideSupportedTissuesRetainsNotApplicableStatus() {
        // This is the existing model's tissue scope, not proof of biological immunity.
        val unsupportedStages = PhenologicalStage.entries.filter {
            BotrytisHostTissueMapper.map(it) == BotrytisHostTissue.UNSUPPORTED
        }
        assertTrue(unsupportedStages.isNotEmpty())
        unsupportedStages.forEach { stage ->
            val result = evaluate(stage, dryWeather()).result
            assertEquals(DiseaseRiskEvaluationStatus.NOT_APPLICABLE, result.evaluationStatus, stage.name)
            assertNull(result.nativeRiskValue, stage.name)
        }
    }

    @Test
    fun supportedStagesStillCalculateWithKnownDryWeather() {
        listOf(
            PhenologicalStage.FLOWERING,
            PhenologicalStage.VERAISON,
            PhenologicalStage.RIPENING,
            PhenologicalStage.HARVEST
        ).forEach { stage ->
            val result = evaluate(stage, dryWeather()).result
            assertEquals(DiseaseRiskEvaluationStatus.CALCULATED, result.evaluationStatus, stage.name)
            assertEquals(0.0, result.nativeRiskValue, stage.name)
            assertEquals(DiseaseRiskLevel.NONE, result.riskLevel, stage.name)
        }
    }

    @Test
    fun supportedStageWithoutWeatherRemainsInsufficientData() {
        val result = evaluate(PhenologicalStage.FLOWERING, emptyList()).result
        assertEquals(DiseaseRiskEvaluationStatus.INSUFFICIENT_DATA, result.evaluationStatus)
        assertNull(result.nativeRiskValue)
    }

    @Test
    fun wetnessTouchingEitherBoundaryMakesTheDayInsufficient() {
        val wetRuns = listOf(0..3, 20..23, 0..23)
        for (run in wetRuns) {
            val result = evaluate(PhenologicalStage.FLOWERING, wetWeather(run)).result
            assertEquals(DiseaseRiskEvaluationStatus.INSUFFICIENT_DATA, result.evaluationStatus)
            assertNull(result.nativeRiskValue)
        }
    }

    @Test
    fun aWetPeriodBetweenDryHoursCanBeEvaluated() {
        val result = evaluate(PhenologicalStage.FLOWERING, wetWeather(5..12)).result
        assertEquals(DiseaseRiskEvaluationStatus.CALCULATED, result.evaluationStatus)
        assertTrue(checkNotNull(result.nativeRiskValue) > 0.0)
    }

    @Test
    fun missingWetnessAndMissingHoursDoNotLookLikeDryWeather() {
        val dry = dryWeather()
        val unknown = dry.mapIndexed { index, point ->
            if (index == 8) point.copy(leafWetness = null) else point
        }
        val gap = dry.filterIndexed { index, _ -> index != 8 }
        for (weather in listOf(unknown, gap)) {
            val result = evaluate(PhenologicalStage.FLOWERING, weather).result
            assertEquals(DiseaseRiskEvaluationStatus.INSUFFICIENT_DATA, result.evaluationStatus)
            assertNull(result.nativeRiskValue)
        }
    }

    @Test
    fun flowerAndBerryEstimatesMatchIndependentEquationFixtures() {
        assertEstimate(PhenologicalStage.FLOWERING, 23.7, 2, 86.63517205754418)
        assertEstimate(PhenologicalStage.FLOWERING, 13.2, 2, 26.20237652518889)
        assertEstimate(PhenologicalStage.RIPENING, 20.8, 14, 63.73946573044291)
        assertEstimate(PhenologicalStage.RIPENING, 11.9, 14, 12.82822233310305)
    }

    private fun assertEstimate(
        stage: PhenologicalStage,
        temperature: Double,
        hours: Int,
        expected: Double
    ) {
        val result = evaluate(stage, wetWeather(5 until 5 + hours, temperature)).result
        assertEquals(DiseaseRiskEvaluationStatus.CALCULATED, result.evaluationStatus)
        assertEquals(expected, checkNotNull(result.nativeRiskValue), absoluteTolerance = 1e-9)
    }

    private fun wetWeather(run: IntRange, temperature: Double = 20.0) =
        dryWeather().mapIndexed { index, point ->
            point.copy(
                temperatureCelsius = temperature,
                leafWetness = checkNotNull(point.leafWetness).copy(value = index in run)
            )
        }

    private fun evaluate(stage: PhenologicalStage?, weather: List<DiseaseRiskWeatherPoint>) =
        model.evaluate(
            input = DiseaseRiskInput(
                diseaseId = 1L,
                vineyardId = 2L,
                blockId = 3L,
                timeZone = "Europe/Sofia",
                hourlyWeather = weather,
                dailyWeather = emptyList(),
                hostContext = DiseaseRiskHostContext(
                    phenologicalStage = stage,
                    activeOccurrence = null,
                    observedSymptoms = emptyList()
                )
            ),
            previousState = null,
            calculatedAt = 86_400_000L
        )

    private fun dryWeather() = List(24) { hour ->
        DiseaseRiskWeatherPoint(
            timestamp = hour.toLong() * 3_600_000L,
            temperatureCelsius = 20.0,
            relativeHumidityPercent = 50.0,
            dewPointCelsius = 9.0,
            precipitationMm = 0.0,
            soilMoisture0To1Cm = null,
            soilMoisture1To3Cm = null,
            leafWetness = EnvironmentalMeasurement(
                value = false,
                source = MeasurementSource.LOCAL_SENSOR,
                confidence = MeasurementConfidence.HIGH
            ),
            isDay = true
        )
    }
}
