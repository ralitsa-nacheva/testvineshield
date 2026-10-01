package com.rncoding.testvineshield.core.domain.time

import kotlinx.datetime.LocalDate

interface AppClock {
    fun nowMillis(): Long

    fun today(
        timeZoneId: String
    ): LocalDate
}