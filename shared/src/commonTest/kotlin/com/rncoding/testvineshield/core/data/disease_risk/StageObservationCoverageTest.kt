package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskHostContext
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherWindow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class StageObservationCoverageTest {
    private val zone = TimeZone.of("Europe/Sofia")
    private val host = DiseaseRiskHostContext(
        phenologicalStage = PhenologicalStage.FLOWERING,
        activeOccurrence = null,
        observedSymptoms = emptyList(),
        stageObservedOn = LocalDate(2026, 3, 29)
    )

    @Test
    fun localSpringClockChangeDayEndsAtExclusiveMidnight() {
        val day = window("2026-03-28T22:00:00Z", "2026-03-29T21:00:00Z")
        assertTrue(host.hasStageObservationFor(day, zone))
        assertFalse(host.hasStageObservationFor(day, TimeZone.UTC))
    }

    @Test
    fun missingDatesStagesAndOtherDaysCannotSupplyEvidence() {
        val day = window("2026-03-28T22:00:00Z", "2026-03-29T21:00:00Z")
        assertFalse(host.copy(stageObservedOn = null).hasStageObservationFor(day, zone))
        assertFalse(host.copy(phenologicalStage = null).hasStageObservationFor(day, zone))
        assertFalse(host.copy(stageObservedOn = LocalDate(2026, 3, 28)).hasStageObservationFor(day, zone))
        assertFalse(host.copy(stageObservedOn = LocalDate(2026, 3, 30)).hasStageObservationFor(day, zone))
    }

    @Test
    fun aRollingWindowCrossingMidnightAndAnEmptyWindowAreRejected() {
        val rolling = window("2026-03-29T09:00:00Z", "2026-03-30T09:00:00Z")
        assertFalse(host.hasStageObservationFor(rolling, zone))
        val empty = rolling.copy(endTimestamp = rolling.startTimestamp)
        assertFalse(host.hasStageObservationFor(empty, zone))
    }

    private fun window(start: String, end: String) = DiseaseRiskWeatherWindow(
        Instant.parse(start).toEpochMilliseconds(),
        Instant.parse(end).toEpochMilliseconds()
    )
}
