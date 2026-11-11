package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskHostContext
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherWindow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** A date-only observation can be matched to one vineyard-local calendar day. */
internal fun DiseaseRiskWeatherWindow.singleLocalDate(timeZone: TimeZone): LocalDate? {
    if (startTimestamp >= endTimestamp) return null

    val firstDate = Instant.fromEpochMilliseconds(startTimestamp)
        .toLocalDateTime(timeZone).date
    // The window end is exclusive, including when it falls at local midnight.
    val lastDate = Instant.fromEpochMilliseconds(endTimestamp - 1)
        .toLocalDateTime(timeZone).date
    return firstDate.takeIf { it == lastDate }
}

internal fun DiseaseRiskHostContext.hasStageObservationFor(
    window: DiseaseRiskWeatherWindow,
    timeZone: TimeZone
): Boolean = phenologicalStage != null &&
    stageObservedOn?.let { it == window.singleLocalDate(timeZone) } == true
