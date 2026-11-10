package com.rncoding.testvineshield.core.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Shared by production history/forecast repositories, including their worker callers.
 * Bounded stripes avoid an ever-growing map of vineyard IDs. Colliding vineyards queue.
 * This coordinates one app process; it does not replace a database location-revision guard.
 */
class WeatherRefreshCoordinator {
    private val stripes = List(16) { Mutex() }
    suspend fun <T> withRefresh(vineyardId: Long, action: suspend () -> T): T {
        require(vineyardId > 0)
        return stripes[(vineyardId % stripes.size).toInt()].withLock { action() }
    }
}
