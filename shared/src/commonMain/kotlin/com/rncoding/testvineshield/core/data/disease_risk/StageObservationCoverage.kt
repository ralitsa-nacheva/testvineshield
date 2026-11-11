package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskHostContext
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherWindow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** A date-only observation can be matched to one vineyard-local calendar day. */
internal fun DiseaseRiskHostContext.hasStageObservationFor(
    window: DiseaseRiskWeatherWindow,
    timeZone: TimeZone
): Boolean {
    val observedOn = stageObservedOn ?: return false
    if (phenologicalStage == null || window.startTimestamp >= window.endTimestamp) return false

    val firstDate = Instant.fromEpochMilliseconds(window.startTimestamp)
        .toLocalDateTime(timeZone).date
    if (firstDate != observedOn) return false

    // The window end is exclusive, including when it falls at local midnight.
    val lastDate = Instant.fromEpochMilliseconds(window.endTimestamp - 1)
        .toLocalDateTime(timeZone).date
    return lastDate == observedOn
}
