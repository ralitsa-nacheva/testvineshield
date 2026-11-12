package com.rncoding.testvineshield.core.domain.disease_risk.powdery

import kotlinx.datetime.LocalDate
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

    private fun evaluate(hours: List<Double>, initialIndex: Int = 60): PowderyMildewDailyRiskResult =
        calculator.evaluateDay(
            day = PowderyMildewDailyTemperature(date, hours + List(24 - hours.size) { 15.0 }),
            previousState = PowderyMildewRiskState(initiated = true, currentIndex = initialIndex)
        ).dailyResult
}
