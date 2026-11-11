package com.rncoding.testvineshield.core.domain.disease_risk

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal data class VineyardWeatherDay(val date: LocalDate, val window: DiseaseRiskWeatherWindow)

internal fun previousCompleteVineyardDay(now: Long, zone: TimeZone): VineyardWeatherDay {
    val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(zone).date
    val previous = today.plus(-1, DateTimeUnit.DAY)
    return VineyardWeatherDay(
        date = previous,
        window = DiseaseRiskWeatherWindow(
            startTimestamp = previous.atStartOfDayIn(zone).toEpochMilliseconds(),
            endTimestamp = today.atStartOfDayIn(zone).toEpochMilliseconds()
        )
    )
}
