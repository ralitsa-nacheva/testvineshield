package com.rncoding.testvineshield.core.domain.disease_risk.downy

import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.disease_risk.*
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class DownyMildewSecondaryEvidenceTest {
    private val model = DownyMildewSecondaryDiseaseRiskModel()
    private val calculator = DownyMildewSecondaryInfectionCalculator()

    @Test
    fun missingHumidityThatCouldCompleteFourHoursIsInsufficient() {
        val hours = List(4) { hour -> point(hour, humidity = if (hour == 2) null else 99.0) }
        val result = evaluate(hours)
        assertEquals(DiseaseRiskEvaluationStatus.INSUFFICIENT_DATA, result.evaluationStatus)
        assertNull(result.event)
    }

    @Test
    fun knownThresholdFailuresAndDaylightRuleOutSporulation() {
        val hours = listOf(
            point(0), point(1, temperature = 13.0), point(2, humidity = 98.0),
            point(3, humidity = null, isDay = true), point(4)
        )
        val result = calculator.evaluate(DownyMildewSecondaryInput(hours, activeOilSpotsObserved = true))
        assertFalse(result.sporulationConditionsMet)
        assertEquals(false, result.infectionConditionsMet)
        assertEquals(DiseaseRiskEvaluationStatus.CALCULATED, evaluate(hours).evaluationStatus)
    }

    @Test
    fun missingHoursRemainInsufficientAndObservedSporulationRemainsRecognizable() {
        assertEquals(
            DiseaseRiskEvaluationStatus.INSUFFICIENT_DATA,
            evaluate(listOf(point(0), point(2))).evaluationStatus
        )
        val result = evaluate(List(4) { point(it) })
        assertEquals(DiseaseRiskEvaluationStatus.INSUFFICIENT_DATA, result.evaluationStatus)
        assertEquals(DiseaseRiskEvent.SECONDARY_SPORULATION_CONDITIONS, result.event)
    }

    @Test
    fun dateOnlyObservationMustPredateTheEarliestVineyardLocalWeatherDate() {
        val start = Instant.parse("2026-06-15T21:00:00Z").toEpochMilliseconds()
        val hours = List(4) { hour -> point(hour).copy(timestamp = start + hour * 3_600_000L) }
        for (date in listOf(null, LocalDate(2026, 6, 16), LocalDate(2026, 6, 17))) {
            val result = evaluate(hours.reversed(), observedOn = date, zone = "Europe/Sofia")
            assertEquals(DiseaseRiskEvaluationStatus.INSUFFICIENT_DATA, result.evaluationStatus)
            assertNull(result.event)
        }
        val earlier = evaluate(hours, observedOn = LocalDate(2026, 6, 15), zone = "Europe/Sofia")
        assertEquals(DiseaseRiskEvent.SECONDARY_SPORULATION_CONDITIONS, earlier.event)
    }

    private fun evaluate(
        hours: List<DiseaseRiskWeatherPoint>,
        observedOn: LocalDate? = LocalDate(1969, 12, 31),
        zone: String = "UTC"
    ): DiseaseRiskResult =
        model.evaluate(
            input = DiseaseRiskInput(
                diseaseId = 1, vineyardId = 2, blockId = 3, timeZone = zone,
                hourlyWeather = hours, dailyWeather = emptyList(),
                hostContext = DiseaseRiskHostContext(
                    phenologicalStage = null, activeOccurrence = null,
                    observedSymptoms = listOf(
                        DiseaseRiskSymptomEvidence(
                            1, DownyMildewSecondaryDiseaseRiskModel.DOWNY_MILDEW_OIL_SPOT_CODE,
                            "Oil spot", "Leaf", PhenologicalStage.FLOWERING, observedOn, null, null
                        )
                    )
                )
            ),
            previousState = null,
            calculatedAt = 86_400_000L
        ).result

    private fun point(
        hour: Int, temperature: Double = 14.0, humidity: Double? = 99.0, isDay: Boolean? = false
    ) = DiseaseRiskWeatherPoint(
        hour * 3_600_000L, temperature, humidity, null, null, null, null, null, isDay
    )
}
