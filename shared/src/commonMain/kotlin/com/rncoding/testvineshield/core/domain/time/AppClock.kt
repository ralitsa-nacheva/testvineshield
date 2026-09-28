package com.rncoding.testvineshield.core.domain.time

interface AppClock {
    fun nowMillis(): Long
}