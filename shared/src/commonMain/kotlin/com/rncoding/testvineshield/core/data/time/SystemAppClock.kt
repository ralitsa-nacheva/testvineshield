package com.rncoding.testvineshield.core.data.time

import com.rncoding.testvineshield.core.domain.time.AppClock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class SystemAppClock : AppClock {

    override fun nowMillis(): Long =
        Clock.System
            .now()
            .toEpochMilliseconds()

    override fun today(
        timeZoneId: String
    ): LocalDate {

        return Clock.System.todayIn(
            TimeZone.of(timeZoneId)
        )
    }
}