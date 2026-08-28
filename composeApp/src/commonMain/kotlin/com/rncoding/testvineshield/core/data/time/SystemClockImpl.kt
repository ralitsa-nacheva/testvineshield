package com.rncoding.testvineshield.core.data.time

import com.rncoding.testvineshield.core.domain.time.SystemClock
import kotlin.time.Clock

class SystemClockImpl: SystemClock {
    override fun now(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }
}