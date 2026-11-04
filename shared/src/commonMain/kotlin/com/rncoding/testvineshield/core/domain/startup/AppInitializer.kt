package com.rncoding.testvineshield.core.domain.startup

/** Shared prerequisite for application screens and background entry points. */
fun interface AppInitializer {
    /** Returns only when ready; failures and cancellation propagate to the caller. */
    suspend fun ensureInitialized()
}
