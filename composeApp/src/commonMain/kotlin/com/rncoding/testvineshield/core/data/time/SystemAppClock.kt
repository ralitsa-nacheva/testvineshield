package com.rncoding.testvineshield.core.data.time

import com.rncoding.testvineshield.core.domain.time.AppClock
import kotlin.time.Clock

class SystemAppClock : AppClock {

    override fun nowMillis(): Long =
        Clock.System.now()
            .toEpochMilliseconds()
}