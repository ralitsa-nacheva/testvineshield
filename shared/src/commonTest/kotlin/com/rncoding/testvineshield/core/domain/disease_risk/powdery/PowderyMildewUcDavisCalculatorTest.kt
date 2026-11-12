package com.rncoding.testvineshield.core.domain.disease_risk.powdery

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEvent
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskHostContext
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskInput
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.test.assertNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PowderyMildewUcDavisCalculatorTest {
    private val calculator = PowderyMildewUcDavisCalculator()
    private val date = LocalDate(2026, 6, 12)

    @Test
    fun aHotDayWithoutSixConduciveHoursLosesOnlyTenPoints() {
        val result = evaluate(listOf(38.0, 38.0))
        assertTrue(result.highTemperaturePenalty)
        assertFalse(result.qualifyingDay)
        assertEquals(50, result.riskIndex)
    }

    @Test
    fun twoConsecutiveHotHoursReduceTheQualifyingDayGain() {
        val conducive = List(6) { 25.0 }
        val contiguous = evaluate(conducive + listOf(38.0, 38.0))
        assertTrue(contiguous.highTemperaturePenalty)
        assertEquals(70, contiguous.riskIndex)

        val separated = evaluate(conducive + listOf(38.0, 37.9, 38.0))
        assertFalse(separated.highTemperaturePenalty)
        assertEquals(80, separated.riskIndex)
    }

    @Test
    fun dailyChangesRespectIndexBounds() {
        assertEquals(0, evaluate(listOf(38.0, 38.0), initialIndex = 5).riskIndex)
        assertEquals(100, evaluate(List(6) { 25.0 }, initialIndex = 95).riskIndex)
    }

    @Test
    fun initiationPointsAreSeparateFromTheRiskIndexAndResetAfterAnInterruption() {
        val model = PowderyMildewUcDavisDiseaseRiskModel()
        val dayMillis = 86_400_000L
        val start = Instant.parse("2026-06-12T00:00:00Z").toEpochMilliseconds()
        var state: PowderyMildewRiskState? = null
        val temperatures = listOf(25.0, 25.0, 15.0, 25.0, 25.0, 25.0)
        val progress = listOf(20.0, 40.0, 0.0, 20.0, 40.0)
        temperatures.forEachIndexed { day, temperature ->
            val midnight = start + day * dayMillis
            val result = model.evaluate(
                input = DiseaseRiskInput(
                    diseaseId = 1L,
                    vineyardId = 2L,
                    blockId = 3L,
                    timeZone = "UTC",
                    hourlyWeather = List(24) { hour ->
                        DiseaseRiskWeatherPoint(
                            midnight + hour * 3_600_000L, temperature,
                            null, null, null, null, null, null, null
                        )
                    },
                    dailyWeather = emptyList(),
                    hostContext = DiseaseRiskHostContext(null, null, emptyList()),
                    monitoringStartDate = date
                ),
                previousState = state,
                calculatedAt = midnight + dayMillis
            )
            state = result.nextState as PowderyMildewRiskState
            if (day < progress.size) {
                assertFalse(checkNotNull(state).initiated)
                assertNull(result.result.nativeRiskValue)
                assertNull(result.result.event)
                assertEquals(progress[day], result.result.factors.single {
                    it.name == "uc_davis_initiation_progress"
                }.value)
            } else {
                assertTrue(checkNotNull(state).initiated)
                assertEquals(60.0, result.result.nativeRiskValue)
                assertEquals(DiseaseRiskEvent.CONIDIAL_RISK_INITIATED, result.result.event)
            }
        }
    }

    private fun evaluate(hours: List<Double>, initialIndex: Int = 60): PowderyMildewDailyRiskResult =
        calculator.evaluateDay(
            day = PowderyMildewDailyTemperature(date, hours + List(24 - hours.size) { 15.0 }),
            previousState = PowderyMildewRiskState(initiated = true, currentIndex = initialIndex)
        ).dailyResult
}
