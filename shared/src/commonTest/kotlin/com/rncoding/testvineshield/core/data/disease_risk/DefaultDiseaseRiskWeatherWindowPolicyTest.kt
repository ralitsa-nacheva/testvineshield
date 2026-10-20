package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultDiseaseRiskWeatherWindowPolicyTest {

    private val policy = DefaultDiseaseRiskWeatherWindowPolicy()

    private val oneHourMillis = 3_600_000L

    private fun localTimestamp(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        zoneId: String
    ): Long {
        return LocalDateTime(
            year, month, day, hour, 0
        ).toInstant(TimeZone.of(zoneId))
            .toEpochMilliseconds()
    }

    @Test
    fun ucDavisNormalDayContains24Hours() {
        val zone = "Europe/Sofia"

        val window = policy.getWindow(
            modelType = DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS,
            calculatedAt = localTimestamp(2026, 6, 16, 12, zone),
            timeZone = zone,
            monitoringStartDate = LocalDate(2026, 6, 15),
            previousState = null
        )

        assertEquals(24L, (window.endTimestamp - window.startTimestamp) / oneHourMillis)
    }

    @Test
    fun downyPrimaryPreservesHalfHourMidnight() {
        val zone = "Asia/Kolkata"

        val window = policy.getWindow(
            modelType = DiseaseRiskModelType.DOWNY_MILDEW_PRIMARY,
            calculatedAt = localTimestamp(2026, 6, 16, 12, zone),
            timeZone = zone,
            monitoringStartDate = LocalDate(2026, 6, 15),
            previousState = null
        )

        val expectedStart = LocalDate(2026, 6, 15)
            .atStartOfDayIn(TimeZone.of(zone))
            .toEpochMilliseconds()

        assertEquals(expectedStart, window.startTimestamp)

        // New Delhi's hourly grid is offset by 30 minutes from UTC hours.
        assertEquals(
            30L * 60_000L,
            window.startTimestamp % oneHourMillis
        )

        // Both window boundaries must use the same hourly grid.
        assertEquals(
            window.startTimestamp % oneHourMillis,
            window.endTimestamp % oneHourMillis
        )

        // The requested duration must contain whole hourly intervals.
        assertEquals(
            0L,
            (window.endTimestamp - window.startTimestamp) % oneHourMillis
        )
    }

    @Test
    fun statelessWindowUsesVineyardHourlyGrid() {
        val zone = "Asia/Kolkata"

        val calculatedAt = localTimestamp(
            2026, 6, 16, 12, zone
        )

        val window = policy.getWindow(
            modelType = DiseaseRiskModelType.BOTRYTIS,
            calculatedAt = calculatedAt,
            timeZone = zone,
            monitoringStartDate = null,
            previousState = null
        )

        assertEquals(
            24L * oneHourMillis,
            window.endTimestamp - window.startTimestamp
        )

        assertEquals(
            30L * 60_000L,
            window.startTimestamp % oneHourMillis
        )

        assertEquals(
            window.startTimestamp % oneHourMillis,
            window.endTimestamp % oneHourMillis
        )
    }

    @Test
    fun statelessWindowAcrossSpringDstContains24Hours() {
        val zone = "Europe/Sofia"

        val calculatedAt = localTimestamp(
            2026, 3, 30, 12, zone
        )

        val window = policy.getWindow(
            modelType = DiseaseRiskModelType.BOTRYTIS,
            calculatedAt = calculatedAt,
            timeZone = zone,
            monitoringStartDate = null,
            previousState = null
        )

        assertEquals(
            24L * oneHourMillis,
            window.endTimestamp - window.startTimestamp
        )

        assertEquals(
            0L,
            window.startTimestamp % oneHourMillis
        )

        assertEquals(
            0L,
            window.endTimestamp % oneHourMillis
        )
    }

    @Test
    fun statelessWindowAcrossAutumnDstContains24Hours() {
        val zone = "Europe/Sofia"

        val calculatedAt = localTimestamp(
            2026, 10, 26, 12, zone
        )

        val window = policy.getWindow(
            modelType = DiseaseRiskModelType.DOWNY_MILDEW_SECONDARY,
            calculatedAt = calculatedAt,
            timeZone = zone,
            monitoringStartDate = null,
            previousState = null
        )

        assertEquals(
            24L * oneHourMillis,
            window.endTimestamp - window.startTimestamp
        )

        assertEquals(
            0L,
            window.startTimestamp % oneHourMillis
        )

        assertEquals(
            0L,
            window.endTimestamp % oneHourMillis
        )
    }

    @Test
    fun statelessWindowAcrossDstHasValidHourlyDuration() {
        val zone = "Europe/Sofia"

        val calculatedAt = localTimestamp(
            2026, 3, 29, 12, zone
        )

        val window = policy.getWindow(
            modelType = DiseaseRiskModelType.POWDERY_MILDEW_ASCOSPORE,
            calculatedAt = calculatedAt,
            timeZone = zone,
            monitoringStartDate = null,
            previousState = null
        )

        val expectedHours =
            (window.endTimestamp - window.startTimestamp) / oneHourMillis

        assertEquals(24L, expectedHours)

        // Verify that each expected observation timestamp fits the window.
        val timestamps = (0 until 24).map { index ->
            window.startTimestamp + index * oneHourMillis
        }

        assertEquals(window.startTimestamp, timestamps.first())

        assertEquals(
            window.endTimestamp - oneHourMillis,
            timestamps.last()
        )

        assertEquals(
            true,
            timestamps.zipWithNext().all { (previous, current) ->
                current - previous == oneHourMillis
            }
        )
    }

    @Test
    fun lordHoweDstChangesHourlyGridOffset() {
        val zone = TimeZone.of("Australia/Lord_Howe")

        val beforeTransition = LocalDate(2026, 10, 3)
            .atStartOfDayIn(zone)
            .toEpochMilliseconds()

        val afterTransition = LocalDate(2026, 10, 5)
            .atStartOfDayIn(zone)
            .toEpochMilliseconds()

        val beforeOffset = beforeTransition % oneHourMillis
        val afterOffset = afterTransition % oneHourMillis

        // Lord Howe advances its clocks by 30 minutes.
        // Its local-midnight hourly grid changes relative to UTC.
        assertEquals(
            30L * 60_000L,
            beforeOffset
        )

        assertEquals(
            0L,
            afterOffset
        )
    }
}