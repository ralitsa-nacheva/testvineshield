package com.rncoding.testvineshield.core.data.startup

import com.rncoding.testvineshield.core.data.local.database.seed.DiseaseCatalogueSeeder
import com.rncoding.testvineshield.core.domain.startup.AppInitializer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultAppInitializer(
    private val diseaseCatalogueSeeder: DiseaseCatalogueSeeder
) : AppInitializer {
    private val mutex = Mutex()

    // Accessed only while holding mutex. Cache successful completion, never failure.
    private var initialized = false

    override suspend fun ensureInitialized() {
        currentCoroutineContext().ensureActive()
        mutex.withLock {
            currentCoroutineContext().ensureActive()
            if (initialized) return@withLock

            diseaseCatalogueSeeder.seed()
            currentCoroutineContext().ensureActive()
            initialized = true
        }
    }
}
